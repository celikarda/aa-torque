package com.aatorque.prefs

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.aatorque.stats.R
import com.aatorque.stats.TorqueServiceWrapper
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsDashboard : PreferenceFragmentCompat() {

    lateinit var performanceTitle: EditTextPreference
    lateinit var stylePreference: ListPreference
    lateinit var mainCat: PreferenceCategory
    lateinit var optionsCat: PreferenceCategory

    private fun gaugeTitle(style: DashboardStyle, index: Int): String {
        return when (style) {
            DashboardStyle.CLASSIC -> when (index) {
                0 -> getString(R.string.pref_leftclock)
                1 -> getString(R.string.pref_centerclock)
                2 -> getString(R.string.pref_rightclock)
                else -> "Gauge ${index + 1}"
            }

            DashboardStyle.GR -> arrayOf(
                "Top Left Gauge",
                "Top Right Gauge",
                "Bottom Left Gauge",
                "Bottom Right Gauge",
            ).getOrElse(index) { "Gauge ${index + 1}" }

            DashboardStyle.DENSE -> "Gauge ${index + 1}"
        }
    }

    private fun displayTitle(style: DashboardStyle, index: Int): String {
        return if (style == DashboardStyle.CLASSIC) {
            resources.getString(R.string.pref_view, index + 1)
        } else {
            arrayOf(
                "FL PSI", "FL Temp",
                "FR PSI", "FR Temp",
                "RL PSI", "RL Temp",
                "RR PSI", "RR Temp"
            ).getOrElse(index) { resources.getString(R.string.pref_view, index + 1) }
        }
    }

    private fun gaugeIcon(index: Int): Int {
        val icons = arrayOf(
            R.drawable.ic_settings_clockl,
            R.drawable.ic_settings_clockc,
            R.drawable.ic_settings_clockr,
            R.drawable.ic_settings_clockl,
            R.drawable.ic_settings_clockc,
            R.drawable.ic_settings_clockr,
        )
        return icons.getOrElse(index) { R.drawable.ic_settings_clockc }
    }

    private fun displayIcon(style: DashboardStyle, index: Int): Int {
        if (style == DashboardStyle.CLASSIC) {
            val classic = arrayOf(
                R.drawable.ic_settings_view1,
                R.drawable.ic_settings_view2,
                R.drawable.ic_settings_view3,
                R.drawable.ic_settings_view4,
            )
            return classic.getOrElse(index) { R.drawable.ic_settings_view1 }
        }
        return R.drawable.ic_tyre
    }

    var mBound = false

    var torqueConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mBound = true
            val torqueService = (service as TorqueServiceWrapper.LocalBinder).getService()
            torqueService.loadPidInformation(false) { pids ->
                activity?.runOnUiThread {
                    mainCat.title = null
                    val dbIndex = dashboardIndex()
                    lifecycleScope.launch {
                        requireContext().dataStore.data.collect { userPreference ->
                            val screen = userPreference.getScreens(dbIndex)
                            val style = styleFromScreen(screen)
                            val (gaugeSlots, displaySlots) = slotsForStyle(style)

                            performanceTitle.text = screen.title
                            performanceTitle.title = resources.getString(R.string.pref_title_performance, dbIndex + 1)
                            performanceTitle.isVisible = style == DashboardStyle.CLASSIC
                            stylePreference.value = style.prefValue
                            optionsCat.removeAll()

                            for (i in 0 until gaugeSlots) {
                                val screenData = screen.getGauges(i)
                                optionsCat.addPreference(
                                    Preference(requireContext()).also {
                                        it.key = "clock_${dbIndex}_$i"
                                        it.summary = pids.firstOrNull { pid ->
                                            "torque_${pid.first}" == screenData.pid
                                        }?.second?.get(0) ?: ""
                                        it.title = gaugeTitle(style, i)
                                        it.icon = AppCompatResources.getDrawable(requireContext(), gaugeIcon(i))
                                        DrawableCompat.setTint(it.icon!!, resources.getColor(R.color.tintColor, requireContext().theme))
                                        it.fragment = SettingsPIDFragment::class.java.canonicalName
                                        it.extras.putString("dashboardStyle", style.prefValue)
                                    }
                                )
                            }

                            for (i in 0 until displaySlots) {
                                val screenData = screen.getDisplays(i)
                                optionsCat.addPreference(
                                    Preference(requireContext()).also {
                                        it.key = "display_${dbIndex}_$i"
                                        it.summary = pids.firstOrNull { pid ->
                                            "torque_${pid.first}" == screenData.pid
                                        }?.second?.get(0) ?: ""
                                        it.title = displayTitle(style, i)
                                        it.icon = AppCompatResources.getDrawable(requireContext(), displayIcon(style, i))
                                        DrawableCompat.setTint(it.icon!!, resources.getColor(R.color.tintColor, requireContext().theme))
                                        it.fragment = SettingsPIDFragment::class.java.canonicalName
                                        it.extras.putString("dashboardStyle", style.prefValue)
                                    }
                                )
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
        stylePreference = findPreference("dashboardStyle")!!

        stylePreference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        stylePreference.setOnPreferenceChangeListener { _, newValue ->
            val newStyle = DashboardStyle.fromPref(newValue as String)
            lifecycleScope.launch {
                requireContext().dataStore.updateData {
                    val idx = dashboardIndex()
                    val updatedScreen = applyStyle(it.getScreens(idx), newStyle)
                    it.toBuilder().setScreens(idx, updatedScreen).build()
                }
            }
            true
        }

        performanceTitle.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        performanceTitle.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                requireContext().dataStore.updateData {
                    val screen = it.getScreens(dashboardIndex()).toBuilder().setTitle(newValue as String)
                    it.toBuilder().setScreens(dashboardIndex(), screen).build()
                }
            }
            true
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
