package com.aatorque.stats

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.aatorque.stats.databinding.FragmentGrGaugeBinding
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class GrTorqueGauge : Fragment() {

    private lateinit var binding: FragmentGrGaugeBinding
    private val mClock: GrGaugeIndicatorView
        get() = binding.dial
    private var minLimit = 0f
    private var maxLimit = 100f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentGrGaugeBinding.inflate(inflater, container, false)
        binding.font = ResourcesCompat.getFont(requireContext(), R.font.open_sans) ?: Typeface.DEFAULT
        return binding.root
    }

    private fun setMinMax(minSpeed: Int, maxSpeed: Int) {
        val minimum = minSpeed.toFloat()
        var maximum = maxSpeed.toFloat()
        if (minimum == maximum) {
            maximum += 1f
        }
        minLimit = minimum.coerceAtMost(maximum)
        maxLimit = maximum.coerceAtLeast(minimum)
        mClock.setRange(minLimit, maxLimit)
        updateTickLabels()
    }

    fun setupClock(data: TorqueData) {
        binding.visible = data.pid != null
        if (data.pid == null) {
            return
        }
        binding.title = data.display.label
        binding.value = "-"
        binding.unit = data.display.unit
        setMinMax(data.display.minValue, data.display.maxValue)
        mClock.setValue(data.display.minValue.toFloat())
        mClock.setPeakValue(data.display.minValue.toFloat())
        data.notifyUpdate = this::onUpdate
    }

    private fun onUpdate(data: TorqueData) {
        val fVal = data.lastData.toFloat()
        mClock.setValue(fVal, TorqueRefresher.REFRESH_INTERVAL.toLong())
        if (data.maxValue.isFinite()) {
            mClock.setPeakValue(data.maxValue.toFloat())
        } else {
            mClock.setPeakValue(fVal)
        }
        binding.value = data.lastDataStr
        binding.unit = data.display.unit
        if (binding.title.isNullOrBlank()) {
            binding.title = "-"
        }
    }

    private fun updateTickLabels() {
        val range = maxLimit - minLimit
        binding.tick0.text = formatTick(minLimit, range)
        binding.tick25.text = formatTick(minLimit + (range * 0.25f), range)
        binding.tick50.text = formatTick(minLimit + (range * 0.50f), range)
        binding.tick75.text = formatTick(minLimit + (range * 0.75f), range)
        binding.tick100.text = formatTick(maxLimit, range)
    }

    private fun formatTick(value: Float, range: Float): String {
        if (abs(value - value.roundToInt()) < 0.001f) {
            return value.roundToInt().toString()
        }
        val decimals = when {
            abs(range) >= 20f -> 0
            abs(range) >= 2f -> 1
            else -> 2
        }
        return "%.${decimals}f".format(Locale.getDefault(), value)
            .replace(Regex("([\\.,]\\d*?)0+$"), "$1")
            .replace(Regex("[\\.,]$"), "")
    }
}
