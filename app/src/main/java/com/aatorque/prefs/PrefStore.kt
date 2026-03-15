package com.aatorque.prefs

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import com.aatorque.datastore.Display
import com.aatorque.datastore.MaxControl
import com.aatorque.datastore.Screen
import com.aatorque.datastore.UserPreference
import java.io.InputStream
import java.io.OutputStream

enum class DashboardStyle(val prefValue: String) {
    CLASSIC("classic"),
    DENSE("dense");

    companion object {
        fun fromPref(value: String?): DashboardStyle {
            return if (value == DENSE.prefValue) DENSE else CLASSIC
        }
    }
}

const val CLASSIC_GAUGE_SLOTS = 3
const val CLASSIC_DISPLAY_SLOTS = 4
const val DENSE_GAUGE_SLOTS = 6
const val DENSE_DISPLAY_SLOTS = 8

private fun gaugeDefaults(index: Int): Display {
    return when (index) {
        0 -> Display.newBuilder()
            .setPid("torque_0c,0")
            .setShowLabel(true)
            .setLabel("RPM")
            .setIcon("ic_cylinder")
            .setMinValue(0)
            .setMaxValue(9000)
            .setUnit("rpm")
            .setWholeNumbers(true)
            .setTicksActive(true)
            .setMaxValuesActive(MaxControl.MAX)
            .setMaxMarksActive(MaxControl.MAX)
            .setChartColor(-12734743)
            .setDisabled(false)
            .build()

        1 -> Display.newBuilder()
            .setPid("torque_0d,0")
            .setShowLabel(true)
            .setLabel("Speed")
            .setIcon("ic_barometer")
            .setMinValue(0)
            .setMaxValue(320)
            .setUnit("km/h")
            .setWholeNumbers(true)
            .setTicksActive(true)
            .setMaxValuesActive(MaxControl.MAX)
            .setMaxMarksActive(MaxControl.MAX)
            .setChartColor(-5314243)
            .setDisabled(false)
            .build()

        2 -> Display.newBuilder()
            .setPid("torque_11,0")
            .setShowLabel(true)
            .setLabel("Throttle")
            .setIcon("ic_throttle")
            .setMinValue(0)
            .setMaxValue(100)
            .setUnit("%")
            .setWholeNumbers(true)
            .setTicksActive(true)
            .setMaxValuesActive(MaxControl.MAX)
            .setMaxMarksActive(MaxControl.MAX)
            .setChartColor(-2659102)
            .setDisabled(false)
            .build()

        3 -> Display.newBuilder()
            .setPid("torque_05,0")
            .setShowLabel(true)
            .setLabel("Coolant")
            .setIcon("ic_water")
            .setMinValue(-40)
            .setMaxValue(140)
            .setUnit("C")
            .setTicksActive(true)
            .setMaxValuesActive(MaxControl.MAX)
            .setMaxMarksActive(MaxControl.MAX)
            .setChartColor(-1476547)
            .setDisabled(false)
            .build()

        4 -> Display.newBuilder()
            .setPid("torque_0f,0")
            .setShowLabel(true)
            .setLabel("Intake")
            .setIcon("ic_outsidetemperature")
            .setMinValue(-40)
            .setMaxValue(120)
            .setUnit("C")
            .setTicksActive(true)
            .setMaxValuesActive(MaxControl.MAX)
            .setMaxMarksActive(MaxControl.MAX)
            .setChartColor(-6726399)
            .setDisabled(false)
            .build()

        else -> Display.newBuilder()
            .setPid("torque_04,0")
            .setShowLabel(true)
            .setLabel("Load")
            .setIcon("ic_powermeter")
            .setMinValue(0)
            .setMaxValue(100)
            .setUnit("%")
            .setWholeNumbers(true)
            .setTicksActive(true)
            .setMaxValuesActive(MaxControl.MAX)
            .setMaxMarksActive(MaxControl.MAX)
            .setChartColor(-10581865)
            .setDisabled(false)
            .build()
    }
}

private fun denseDisplayDefaults(index: Int): Display {
    return when (index) {
        0 -> Display.newBuilder()
            .setPid("torque_0b,0")
            .setShowLabel(false)
            .setLabel("FL PSI")
            .setIcon("ic_none")
            .setMinValue(0)
            .setMaxValue(70)
            .setUnit("psi")
            .setWholeNumbers(true)
            .setDisabled(false)
            .build()

        1 -> Display.newBuilder()
            .setPid("torque_05,0")
            .setShowLabel(false)
            .setLabel("FL Temp")
            .setIcon("ic_none")
            .setMinValue(-20)
            .setMaxValue(180)
            .setUnit("C")
            .setDisabled(false)
            .build()

        2 -> Display.newBuilder()
            .setPid("torque_0b,0")
            .setShowLabel(false)
            .setLabel("FR PSI")
            .setIcon("ic_none")
            .setMinValue(0)
            .setMaxValue(70)
            .setUnit("psi")
            .setWholeNumbers(true)
            .setDisabled(false)
            .build()

        3 -> Display.newBuilder()
            .setPid("torque_05,0")
            .setShowLabel(false)
            .setLabel("FR Temp")
            .setIcon("ic_none")
            .setMinValue(-20)
            .setMaxValue(180)
            .setUnit("C")
            .setDisabled(false)
            .build()

        4 -> Display.newBuilder()
            .setPid("torque_0b,0")
            .setShowLabel(false)
            .setLabel("RL PSI")
            .setIcon("ic_none")
            .setMinValue(0)
            .setMaxValue(70)
            .setUnit("psi")
            .setWholeNumbers(true)
            .setDisabled(false)
            .build()

        5 -> Display.newBuilder()
            .setPid("torque_05,0")
            .setShowLabel(false)
            .setLabel("RL Temp")
            .setIcon("ic_none")
            .setMinValue(-20)
            .setMaxValue(180)
            .setUnit("C")
            .setDisabled(false)
            .build()

        6 -> Display.newBuilder()
            .setPid("torque_0b,0")
            .setShowLabel(false)
            .setLabel("RR PSI")
            .setIcon("ic_none")
            .setMinValue(0)
            .setMaxValue(70)
            .setUnit("psi")
            .setWholeNumbers(true)
            .setDisabled(false)
            .build()

        else -> Display.newBuilder()
            .setPid("torque_05,0")
            .setShowLabel(false)
            .setLabel("RR Temp")
            .setIcon("ic_none")
            .setMinValue(-20)
            .setMaxValue(180)
            .setUnit("C")
            .setDisabled(false)
            .build()
    }
}

private fun classicDisplayDefaults(index: Int): Display {
    return Display.newBuilder().setDisabled(index >= CLASSIC_DISPLAY_SLOTS).build()
}

fun styleFromScreen(screen: Screen): DashboardStyle {
    return if (
        screen.gaugesCount >= DENSE_GAUGE_SLOTS ||
        screen.displaysCount >= DENSE_DISPLAY_SLOTS
    ) {
        DashboardStyle.DENSE
    } else {
        DashboardStyle.CLASSIC
    }
}

fun slotsForStyle(style: DashboardStyle): Pair<Int, Int> {
    return if (style == DashboardStyle.DENSE) {
        DENSE_GAUGE_SLOTS to DENSE_DISPLAY_SLOTS
    } else {
        CLASSIC_GAUGE_SLOTS to CLASSIC_DISPLAY_SLOTS
    }
}

private fun gaugeForStyle(index: Int): Display = gaugeDefaults(index)

private fun displayForStyle(style: DashboardStyle, index: Int): Display {
    return if (style == DashboardStyle.DENSE) denseDisplayDefaults(index) else classicDisplayDefaults(index)
}

fun normalizeScreen(screen: Screen, style: DashboardStyle = styleFromScreen(screen)): Screen {
    val (gaugeSlots, displaySlots) = slotsForStyle(style)
    val builder = screen.toBuilder().clearGauges().clearDisplays()
    if (screen.title.isBlank()) {
        builder.setTitle(if (style == DashboardStyle.DENSE) "Special Dash" else "Performance")
    }

    for (i in 0 until gaugeSlots) {
        val value = if (screen.gaugesCount > i) screen.getGauges(i) else gaugeForStyle(i)
        builder.addGauges(value)
    }
    for (i in 0 until displaySlots) {
        val value = if (screen.displaysCount > i) screen.getDisplays(i) else displayForStyle(style, i)
        builder.addDisplays(value)
    }
    return builder.build()
}

fun applyStyle(screen: Screen, style: DashboardStyle): Screen {
    return normalizeScreen(screen, style)
}

fun normalizePreference(pref: UserPreference): UserPreference {
    val builder = pref.toBuilder().clearScreens()
    if (pref.screensCount == 0) {
        builder.addScreens(normalizeScreen(Screen.newBuilder().setTitle("Performance").build(), DashboardStyle.CLASSIC))
        return builder.build()
    }

    pref.screensList.forEach {
        builder.addScreens(normalizeScreen(it))
    }
    return builder.build()
}

object UserPreferenceSerializer : Serializer<UserPreference> {
    val defaultScreen = normalizeScreen(Screen.newBuilder().setTitle("Performance").build(), DashboardStyle.CLASSIC)

    override var defaultValue = UserPreference.newBuilder()
        .addScreens(defaultScreen)
        .setSelectedTheme("Electro Vehicle")
        .setSelectedFont("ev")
        .setSelectedBackground("background_incar_black")
        .setCenterGaugeLarge(false)
        .build()

    override suspend fun readFrom(input: InputStream): UserPreference {
        try {
            return normalizePreference(UserPreference.parseFrom(input))
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun writeTo(t: UserPreference, output: OutputStream) = t.writeTo(output)
}

val Context.dataStore: DataStore<UserPreference> by dataStore(
    fileName = "user_prefs.pb",
    serializer = UserPreferenceSerializer
)
