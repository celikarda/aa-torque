package com.aatorque.prefs

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.aatorque.stats.R
import com.aatorque.stats.TorqueServiceWrapper
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsDashboard : PreferenceFragmentCompat() {

    lateinit var performanceTitle: EditTextPreference
    lateinit var mainCat: PreferenceCategory
    lateinit var optionsCat: PreferenceCategory

    val clockIcons = arrayOf(
        R.drawable.ic_settings_clockl,
        R.drawable.ic_settings_clockc,
        R.drawable.ic_settings_clockr,
    )
    val displayIcons = arrayOf(
        R.drawable.ic_settings_view1,
        R.drawable.ic_settings_view2,
        R.drawable.ic_settings_view3,
        R.drawable.ic_settings_view4,
    )
    var mBound = false

    private fun gaugeTitle(index: Int): String = when (index) {
        0 -> getString(R.string.pref_leftclock)
        1 -> getString(R.string.pref_centerclock)
        2 -> getString(R.string.pref_rightclock)
        6 -> "Tyre FL Temp"
        7 -> "Tyre FR Temp"
        8 -> "Tyre RL Temp"
        9 -> "Tyre RR Temp"
        else -> "Gauge ${index + 1}"
    }

    private fun displayTitle(index: Int): String = when (index) {
        0 -> "Tyre FL PSI"
        1 -> "Tyre FR PSI"
        2 -> "Tyre RL PSI"
        3 -> "Tyre RR PSI"
        else -> resources.getString(R.string.pref_view, index + 1)
    }

    private fun gaugeIcon(index: Int): Int {
        if (index < clockIcons.size) return clockIcons[index]
        return if (index in 6..9) R.drawable.ic_tyre else R.drawable.ic_settings_clockc
    }

    private fun displayIcon(index: Int): Int {
        if (index < displayIcons.size) return displayIcons[index]
        return R.drawable.ic_tyre
    }

    var torqueConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mBound = true
            val torqueService = (service as TorqueServiceWrapper.LocalBinder).getService()
            torqueService.loadPidInformation(false) { pids ->
                activity?.let {
                    it.runOnUiThread {
                        mainCat.title = null
                                val dbIndex = dashboardIndex()
                                lifecycleScope.launch {
                                    requireContext().dataStore.data.collect { userPreference ->
                                        val screen = userPreference.getScreens(dbIndex)
                                        performanceTitle.text = screen.title
                                        performanceTitle.title = resources.getString(R.string.pref_title_performance, dbIndex + 1)
                                        optionsCat.removeAll()
                                        val sources = arrayOf(screen.gaugesList, screen.displaysList)
                                        val types = arrayOf("clock", "display")
                                        sources.forEachIndexed { i, source ->
                                            val type = types[i]
                                            source.forEachIndexed { j, screenData ->
                                                val iconId = if (type == "clock") {
                                                    gaugeIcon(j)
                                                } else {
                                                    displayIcon(j)
                                                }
                                                optionsCat.addPreference(
                                                    Preference(requireContext()).also {
                                                        it.key = "${type}_${dbIndex}_${j}"
                                                        it.summary = pids.firstOrNull { pid ->
                                                            "torque_${pid.first}" == screenData.pid
                                                        }?.second?.get(0) ?: ""
                                                        it.title = if (type == "clock") {
                                                            gaugeTitle(j)
                                                        } else {
                                                            displayTitle(j)
                                                        }
                                                        it.icon = AppCompatResources.getDrawable(
                                                            requireContext(),
                                                            iconId
                                                        )
                                                        DrawableCompat.setTint(it.icon!!, resources.getColor(R.color.tintColor, requireContext().theme))
                                                        it.fragment = SettingsPIDFragment::class.java.canonicalName
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }
    }

    fun dashboardIndex(): Int {
        return requireArguments().getCharSequence("prefix")?.split("_")!!.last().toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBound = TorqueServiceWrapper.runStartIntent(requireContext(), torqueConnection)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.display_setting)
        mainCat = findPreference("displayCat")!!
        optionsCat = findPreference("displayOptions")!!
        performanceTitle = findPreference("performanceTitle")!!
        performanceTitle.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        performanceTitle.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                requireContext().dataStore.updateData {
                    val screen =
                        it.getScreens(dashboardIndex()).toBuilder().setTitle(newValue as String)
                    return@updateData it.toBuilder().setScreens(dashboardIndex(), screen).build()
                }
            }
            return@setOnPreferenceChangeListener true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBound) {
            try {
                requireActivity().unbindService(torqueConnection)
            } catch (e: IllegalArgumentException) {
                Timber.e("Failed to unbind service", e)
            }
            mBound = false
        }
    }

}
