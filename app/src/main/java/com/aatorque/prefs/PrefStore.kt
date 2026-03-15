package com.aatorque.prefs

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import com.aatorque.datastore.Display
import com.aatorque.datastore.Screen
import com.aatorque.datastore.UserPreference
import com.google.protobuf.TextFormat
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

private const val DEFAULT_GAUGE_SLOTS = 10
private const val DEFAULT_DISPLAY_SLOTS = 4

const val DEFAULT_SETTINGS = """
screens {
  title: "Special Dash"
  gauges {
    pid: "torque_0c,0"
    showLabel: true
    label: "RPM"
    icon: "ic_cylinder"
    minValue: 0
    maxValue: 9000
    unit: "rpm"
    wholeNumbers: true
    ticksActive: true
    maxValuesActive: MAX
    maxMarksActive: MAX
    chartColor: -12734743
    disabled: false
  }
  gauges {
    pid: "torque_0d,0"
    showLabel: true
    label: "Speed"
    icon: "ic_barometer"
    minValue: 0
    maxValue: 320
    unit: "km/h"
    wholeNumbers: true
    ticksActive: true
    maxValuesActive: MAX
    maxMarksActive: MAX
    chartColor: -5314243
    disabled: false
  }
  gauges {
    pid: "torque_05,0"
    showLabel: true
    label: "Coolant"
    icon: "ic_water"
    minValue: -40
    maxValue: 140
    unit: "C"
    ticksActive: true
    maxValuesActive: MAX
    maxMarksActive: MAX
    chartColor: -1476547
    disabled: false
  }
  gauges {
    pid: "torque_0f,0"
    showLabel: true
    label: "Intake"
    icon: "ic_outsidetemperature"
    minValue: -40
    maxValue: 120
    unit: "C"
    ticksActive: true
    maxValuesActive: MAX
    maxMarksActive: MAX
    chartColor: -6726399
    disabled: false
  }
  gauges {
    pid: "torque_11,0"
    showLabel: true
    label: "Throttle"
    icon: "ic_throttle"
    minValue: 0
    maxValue: 100
    unit: "%"
    wholeNumbers: true
    ticksActive: true
    maxValuesActive: MAX
    maxMarksActive: MAX
    chartColor: -2659102
    disabled: false
  }
  gauges {
    pid: "torque_04,0"
    showLabel: true
    label: "Load"
    icon: "ic_powermeter"
    minValue: 0
    maxValue: 100
    unit: "%"
    wholeNumbers: true
    ticksActive: true
    maxValuesActive: MAX
    maxMarksActive: MAX
    chartColor: -10581865
    disabled: false
  }
  gauges {
    pid: "torque_05,0"
    showLabel: true
    label: "FL Temp"
    icon: "ic_tyre"
    minValue: -20
    maxValue: 180
    unit: "C"
    ticksActive: true
    maxValuesActive: MAX
    maxMarksActive: MAX
    chartColor: -14575885
    disabled: false
  }
  gauges {
    pid: "torque_05,0"
    showLabel: true
    label: "FR Temp"
    icon: "ic_tyre"
    minValue: -20
    maxValue: 180
    unit: "C"
    ticksActive: true
    maxValuesActive: MAX
    maxMarksActive: MAX
    chartColor: -14575885
    disabled: false
  }
  gauges {
    pid: "torque_05,0"
    showLabel: true
    label: "RL Temp"
    icon: "ic_tyre"
    minValue: -20
    maxValue: 180
    unit: "C"
    ticksActive: true
    maxValuesActive: MAX
    maxMarksActive: MAX
    chartColor: -14575885
    disabled: false
  }
  gauges {
    pid: "torque_05,0"
    showLabel: true
    label: "RR Temp"
    icon: "ic_tyre"
    minValue: -20
    maxValue: 180
    unit: "C"
    ticksActive: true
    maxValuesActive: MAX
    maxMarksActive: MAX
    chartColor: -14575885
    disabled: false
  }
  displays {
    pid: "torque_0b,0"
    showLabel: true
    label: "FL PSI"
    icon: "ic_tyre"
    minValue: 0
    maxValue: 70
    unit: "psi"
    wholeNumbers: true
    disabled: false
  }
  displays {
    pid: "torque_0b,0"
    showLabel: true
    label: "FR PSI"
    icon: "ic_tyre"
    minValue: 0
    maxValue: 70
    unit: "psi"
    wholeNumbers: true
    disabled: false
  }
  displays {
    pid: "torque_0b,0"
    showLabel: true
    label: "RL PSI"
    icon: "ic_tyre"
    minValue: 0
    maxValue: 70
    unit: "psi"
    wholeNumbers: true
    disabled: false
  }
  displays {
    pid: "torque_0b,0"
    showLabel: true
    label: "RR PSI"
    icon: "ic_tyre"
    minValue: 0
    maxValue: 70
    unit: "psi"
    wholeNumbers: true
    disabled: false
  }
}
selectedTheme: "Electro Vehicle"
selectedFont: "ev"
selectedBackground: "background_incar_black"
centerGaugeLarge: false
"""

object UserPreferenceSerializer : Serializer<UserPreference> {
    val defaultGauge = Display.newBuilder().setShowLabel(true)
    val defaultDisplay = Display.newBuilder().setShowLabel(true)
    val defaultScreen = Screen.newBuilder().apply {
        repeat(DEFAULT_GAUGE_SLOTS) {
            addGauges(defaultGauge)
        }
        repeat(DEFAULT_DISPLAY_SLOTS) {
            addDisplays(defaultDisplay)
        }
    }

    override var defaultValue: UserPreference

    init {
        defaultValue = try {
            TextFormat.parse(
                DEFAULT_SETTINGS,
                UserPreference::class.java
            )
        } catch (e: Exception) {
            Timber.e("Failed to load defaults", e)
            UserPreference.newBuilder().addScreens(defaultScreen).build()
        }
    }

    override suspend fun readFrom(input: InputStream): UserPreference {
        try {
            return UserPreference.parseFrom(input)
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
