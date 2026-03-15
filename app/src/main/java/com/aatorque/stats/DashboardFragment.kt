package com.aatorque.stats

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.MediaMetadata
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.InputDeviceCompat
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aatorque.datastore.Display
import com.aatorque.datastore.UserPreference
import com.aatorque.prefs.SettingsViewModel
import com.aatorque.prefs.dataStore
import com.aatorque.stats.databinding.FragmentDashboardBinding
import com.aatorque.utils.CountDownLatch
import com.google.android.apps.auto.sdk.StatusBarController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs


open class DashboardFragment : AlbumArt() {
    lateinit var rootView: View
    lateinit var mLayoutDashboard: ConstraintLayout

    val torqueRefresher = TorqueRefresher()
    private val torqueService = TorqueService()

    private lateinit var mBtnNext: ImageButton
    private lateinit var mBtnPrev: ImageButton
    private lateinit var mTitleElement: TextView
    private lateinit var mWrapper: RelativeLayout
    lateinit var mConStatus: TextView

    private val gaugeIds = intArrayOf(
        R.id.gauge1,
        R.id.gauge2,
        R.id.gauge3,
        R.id.gauge4,
        R.id.gauge5,
        R.id.gauge6,
        R.id.gauge7,
        R.id.gauge8,
        R.id.gauge9,
        R.id.gauge10,
    )
    private val displayIds = intArrayOf(
        R.id.display1,
        R.id.display2,
        R.id.display3,
        R.id.display4,
    )
    private val gaugeOffset = gaugeIds.size

    private val hiddenGauge = Display.newBuilder().setDisabled(true).build()
    private val hiddenDisplay = Display.newBuilder().setDisabled(true).build()

    var guages = arrayOfNulls<TorqueGauge>(gaugeIds.size)
    var displays = arrayOfNulls<TorqueDisplay>(displayIds.size)
    var gaugeViews = arrayOfNulls<FragmentContainerView>(gaugeIds.size)

    private var screensAnimating = false
    private var mStarted = false
    lateinit var binding: FragmentDashboardBinding
    lateinit var torqueChart: TorqueChart
    lateinit var settingsViewModel: SettingsViewModel

    val viewReady = CountDownLatch(1)
    private var lastBackground: Int = 0
    val albumArtReady = CountDownLatch(2)
    var shouldDisplayArtwork = false
    var displayingArtwork = false
    var albumBlurEffect: RenderEffect? = null
        set(value) {
            if (Build.VERSION.SDK_INT >= 31) {
                if (displayingArtwork) {
                    binding.blurEffect = value
                }
                field = value
            }
        }
        get() {
            return if (Build.VERSION.SDK_INT >= 31) field else null
        }
    var albumColorFilter: PorterDuffColorFilter? = null
        set(value) {
            field = value
            if (displayingArtwork) {
                binding.colorFilter = value
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        val context = requireContext()
        torqueService.startTorque(context)
        val registerWithView = { call: suspend (Flow<UserPreference>) -> Unit ->
            lifecycleScope.launch {
                viewReady.await()
                call(context.dataStore.data)
            }
        }
        registerWithView {
            data -> data.map {
                it.selectedBackground
            }.distinctUntilChanged().collect {
                setupBackground(it)
                albumArtReady.countDown()
            }
        }
        registerWithView {
            data -> data.collect {
                val screenIndex = abs(it.currentScreen) % it.screensCount
                val screens = it.screensList[screenIndex]
                val showChartChanged = binding.showChart != it.showChart
            binding.title = screens.title
            binding.showBtns = false
            settingsViewModel.chartVisible.value = it.showChart
                settingsViewModel.minMaxBelow.value = it.minMaxBelow
                shouldDisplayArtwork = it.albumArt

                albumArtReady.countDown()

                if (it.showChart) {
                    torqueChart.setupItems(
                        screens.gaugesList.take(gaugeIds.size).mapIndexed { index, display ->
                            torqueRefresher.updateIfNeeded(index, screenIndex, display)
                        }.toTypedArray()
                    )
                } else {
                    screens.gaugesList.take(gaugeIds.size).forEachIndexed { index, display ->
                        if (showChartChanged || torqueRefresher.hasChanged(index, display)) {
                            val clock = torqueRefresher.populateQuery(index, screenIndex, display)
                            guages[index]?.setupClock(clock)
                        }
                    }
                    for (index in screens.gaugesList.size until gaugeIds.size) {
                        if (showChartChanged || torqueRefresher.hasChanged(index, hiddenGauge)) {
                            val hidden = torqueRefresher.populateQuery(index, screenIndex, hiddenGauge)
                            guages[index]?.setupClock(hidden)
                        }
                    }
                }
                screens.displaysList.take(displayIds.size).forEachIndexed { index, display ->
                    if (torqueRefresher.hasChanged(index + gaugeOffset, display)) {
                        val td = torqueRefresher.populateQuery(
                            index + gaugeOffset,
                            screenIndex,
                            display
                        )
                        displays[index]?.setupElement(td)
                    }
                }
                for (index in screens.displaysList.size until displayIds.size) {
                    val pos = index + gaugeOffset
                    if (torqueRefresher.hasChanged(pos, hiddenDisplay)) {
                        val hidden = torqueRefresher.populateQuery(pos, screenIndex, hiddenDisplay)
                        displays[index]?.setupElement(hidden)
                    }
                }
                torqueRefresher.makeExecutors(torqueService)
            }
        }
        registerWithView {
            data -> data.map {
                it.opacity
            }.distinctUntilChanged().collect {
                binding.gaugeAlpha = if (it == 0) 1f else 0.01f * it
            }
        }
        registerWithView {
            data -> data.map {
                it.darkenArt
            }.distinctUntilChanged().collect {
                albumColorFilter = if (it != 0) {
                    PorterDuffColorFilter(
                        Color.valueOf(0f, 0f, 0f, it * 0.01f).toArgb(),
                        PorterDuff.Mode.DARKEN,
                    )
                } else null
            }
        }
        if (Build.VERSION.SDK_INT >= 31) {
            registerWithView {
                data -> data.map {
                    it.blurArt
                }.distinctUntilChanged().collect {
                    albumBlurEffect = if (it != 0) {
                        val blurFloat = it.toFloat()
                        RenderEffect.createBlurEffect(
                            blurFloat, blurFloat,
                            Shader.TileMode.MIRROR
                        )
                    } else null
                }
            }
        }
        registerWithView {
            data -> data.map {
                it.selectedFont
            }.distinctUntilChanged().collect(
                this@DashboardFragment::setupTypeface
            )
        }
        registerWithView {
            data -> data.map {
                it.centerGaugeLarge
            }.distinctUntilChanged().collect(
                this@DashboardFragment::updateScale
            )
        }
        registerWithView{
            torqueRefresher.connectStatus.collect {
                binding.status = when (it) {
                    ConnectStatus.CONNECTING_TORQUE -> R.string.status_connecting_torque
                    ConnectStatus.CONNECTING_ECU -> R.string.status_connecting_to_ecu
                    ConnectStatus.SETUP_GAUGE -> R.string.status_setup_gauges
                    ConnectStatus.CONNECTED -> null
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.i("onCreateView")
        binding = FragmentDashboardBinding.inflate(inflater, container, false)

        settingsViewModel.typefaceLiveData.observe(viewLifecycleOwner) {
            binding.font = it
        }
        settingsViewModel.chartVisible.observe(viewLifecycleOwner) {
            binding.showChart = it
        }
        settingsViewModel.minMaxBelow.observe(viewLifecycleOwner) {
            binding.minMaxBelow = it
        }

        rootView = binding.root

        mLayoutDashboard = rootView.findViewById(R.id.layoutDashboard)
        mBtnNext = binding.nextBtn
        mBtnPrev = binding.prevButton
        mBtnNext.setOnClickListener { setScreen(1) }
        mBtnPrev.setOnClickListener  { setScreen(-1) }
        binding.chartBtn.setOnClickListener { toggleShowChart(binding.showChart != true)  }
        mTitleElement = binding.textTitle
        mWrapper = binding.includeWrap
        mConStatus = binding.conStatus
        gaugeIds.forEachIndexed { index, id ->
            gaugeViews[index] = rootView.findViewById(id)
            guages[index] = childFragmentManager.findFragmentById(id) as TorqueGauge
        }
        displayIds.forEachIndexed { index, id ->
            displays[index] = childFragmentManager.findFragmentById(id) as TorqueDisplay
            displays[index]?.isBottomDisplay = false
        }
        torqueChart = childFragmentManager.findFragmentById(R.id.chartFrag)!! as TorqueChart
        val filter = IntentFilter().apply { addAction("KEY_DOWN") }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(p0: Context?, intent: Intent?) {
                    if (intent?.getIntExtra("KEY_CODE", 0) == KeyEvent.KEYCODE_DPAD_CENTER) {
                        toggleShowChart(binding.showChart != true)
                    }
                }
            }, filter)
        val gestureDetector =
            GestureDetector(rootView.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 != null) {
                        val diffX = e2.x - e1.x
                        val diffY = e2.y - e1.y

                        // Set a minimum swipe distance threshold (e.g., 100 pixels)
                        if (abs(diffX) > abs(diffY) && abs(diffX) > 100) {
                            if (diffX > 0) {
                                // Swipe Right
                                setScreen(-1)
                            } else {
                                // Swipe Left
                                setScreen(1)
                            }
                            return true
                        } else if (abs(diffY) > abs(diffX) && abs(diffY) > 100) {
                            toggleShowChart(binding.showChart != true)
                        }
                    }
                    return false
                }
            })
        rootView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick() // Handle accessibility
            }
            true
        }
        configureRotaryInput()
        return rootView
    }


    fun setScreen(direction: Int) {
        if (screensAnimating) return
        screensAnimating = true
        val duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        mTitleElement.animate().alpha(0f).duration = duration
        mWrapper.animate()!!.translationX((rootView.width * -direction).toFloat()).setDuration(
            duration
        ).alpha(0f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                lifecycleScope.launch {
                    requireContext().dataStore.updateData {
                            currentSettings ->
                        currentSettings.toBuilder().setCurrentScreen(
                            (currentSettings.screensCount +
                                    currentSettings.currentScreen +
                                    direction
                                    ) % currentSettings.screensCount
                        ).build()
                    }
                    mWrapper.translationX = (rootView.width * direction).toFloat()
                    mWrapper.alpha = 1f
                    mWrapper.animate().setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            screensAnimating = false
                        }
                    }).translationX(0f).duration = duration
                    mTitleElement.animate().alpha(1f).duration = duration
                }
            }
        })
    }

    fun toggleShowChart(showChart: Boolean) {
        if (screensAnimating) return
        screensAnimating = true
        mWrapper.animate()!!.alpha(0f).setDuration(
            300
        ).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mWrapper.animate()!!.alpha(1f).setDuration(300).setListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            screensAnimating = false
                        }
                    }
                )
                lifecycleScope.launch {
                    context?.dataStore?.updateData { currentSettings ->
                        currentSettings.toBuilder().setShowChart(showChart).build()
                    }
                }
            }
        })
    }

    override fun setupStatusBar(sc: StatusBarController) {
        sc.hideTitle()
    }

    override fun onResume() {
        Timber.d("onResume")
        super.onResume()
        lifecycleScope.launch {
            torqueRefresher.makeExecutors(torqueService)
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            viewReady.countDown()
        }
    }

    override fun onStop() {
        super.onStop()
        mStarted = false
    }

    override fun onPause() {
        Timber.d("onPause")
        super.onPause()
        torqueRefresher.stopExecutors()
    }

    override fun onDestroy() {
        super.onDestroy()
        torqueService.onDestroy(requireContext())
        torqueService.requestQuit(requireContext())
    }

    override suspend fun onMediaChanged(medadata: MediaMetadata?) {
        Timber.i("Got new metadata $medadata shouldDisplay: $shouldDisplayArtwork")
        albumArtReady.await()
        if (!shouldDisplayArtwork) return
        if (medadata != null) {
            binding.backgroundBitmap = metaDataToArt(medadata)
            if (binding.backgroundBitmap != null) {
                binding.blurEffect = albumBlurEffect
                binding.colorFilter = albumColorFilter
                displayingArtwork = true
                return
            }
        }
        setupBackground(lastBackground)
    }

    private fun updateScale(largeCenter: Boolean) {
        binding.largeCenter = largeCenter
    }

    private fun setupBackground(newBackground: String?) {
        lastBackground = context?.let {
            resources.getIdentifier(
                newBackground ?: "background_incar_black",
                "drawable",
                it.packageName
            )
        } ?: lastBackground
        setupBackground(lastBackground)
    }

    private fun setupBackground(resource: Int) {
        binding.blurEffect = null
        binding.colorFilter = null
        displayingArtwork = false
        binding.backgroundResource = resource
    }

    fun configureRotaryInput() {
        rootView.setOnGenericMotionListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_SCROLL &&
                ev.isFromSource(InputDeviceCompat.SOURCE_MOUSE)
            ) {
                val delta = ev.getAxisValue(MotionEvent.AXIS_VSCROLL)
                setScreen(if (delta < 0) 1 else -1)
                true
            } else {
                false
            }
        }
    }

    fun setupTypeface(selectedFont: String) {
        Timber.d("font: $selectedFont")
        val font = when (selectedFont) {
            "segments" -> R.font.digital
            "seat" -> R.font.seat_metastyle_monodigit_regular
            "audi" -> R.font.auditypedisplayhigh
            "vw" -> R.font.vwtextcarui_regular
            "vw2" -> R.font.vwthesis_mib_regular
            "frutiger" -> R.font.frutiger
            "vw3" -> R.font.vw_digit_reg
            "skoda" -> R.font.skoda
            "larabie" -> R.font.larabie
            "ford" -> R.font.unitedsans
            "ev" -> R.font.ev
            else -> R.font.digital
        }
        settingsViewModel.setFont(font)
    }


}
