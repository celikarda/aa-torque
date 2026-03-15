package com.aatorque.stats

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aatorque.datastore.MaxControl
import com.aatorque.prefs.SettingsViewModel
import com.aatorque.stats.databinding.FragmentMiniGaugeBinding
import com.github.anastr.speedviewlib.SpeedView
import com.github.anastr.speedviewlib.Speedometer
import com.github.anastr.speedviewlib.components.Section
import com.github.anastr.speedviewlib.components.indicators.Indicator
import java.util.Locale

class MiniTorqueGauge : Fragment() {

    private lateinit var binding: FragmentMiniGaugeBinding
    private val mClock: SpeedView
        get() = binding.dial
    private val mMax: Speedometer
        get() = binding.dialMax
    lateinit var settingsViewModel: SettingsViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        settingsViewModel = ViewModelProvider(requireParentFragment())[SettingsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMiniGaugeBinding.inflate(inflater, container, false)
        settingsViewModel.typefaceLiveData.observe(viewLifecycleOwner, this::setupTypeface)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mClock.setIndicator(Indicator.Indicators.LineIndicator)
        mClock.indicator.color = Color.WHITE
        mClock.indicator.width = mClock.dpTOpx(2f)
        mClock.isWithIndicatorLight = false
        mClock.textColor = Color.TRANSPARENT
        mClock.clearSections()
        mClock.addSections(Section(0f, 1f, Color.parseColor("#AAFFFFFF")))

        mMax.setIndicator(Indicator.Indicators.LineIndicator)
        mMax.indicator.width = mClock.dpTOpx(1.6f)
        mMax.indicator.color = Color.RED
        mMax.isWithIndicatorLight = false
        mMax.textColor = Color.TRANSPARENT
        mClock.indicator.color = Color.WHITE
        mMax.clearSections()
        mMax.addSections(Section(0f, 1f, Color.TRANSPARENT))
    }

    private fun setupTypeface(typeface: Typeface) {
        binding.font = typeface
        mClock.speedTextTypeface = typeface
        mClock.textTypeface = typeface
        mMax.speedTextTypeface = typeface
        mMax.textTypeface = typeface
    }

    private fun setMinMax(minspeed: Int, maxspeed: Int) {
        val minimum = minspeed.toFloat()
        var maximum = maxspeed.toFloat()
        if (minimum == maximum) {
            maximum += 1f
        }
        binding.minMax = Pair(minimum.coerceAtMost(maximum), maximum.coerceAtLeast(minimum))
    }

    fun setupClock(data: TorqueData) {
        binding.visible = data.pid != null
        data.notifyUpdate = this::onUpdate

        binding.title = if (data.display.showLabel) data.display.label else ""
        mClock.unit = data.display.unit
        mClock.speedTextListener = if (data.display.wholeNumbers) {
            { speed -> "%.0f".format(Locale.getDefault(), speed) }
        } else {
            { speed -> "%.1f".format(Locale.getDefault(), speed) }
        }
        setMinMax(data.display.minValue, data.display.maxValue)
        mClock.setSpeedAt(mClock.minSpeed)
        mMax.setSpeedAt(mMax.minSpeed)
        mMax.visibility = View.INVISIBLE
    }

    private fun onUpdate(data: TorqueData) {
        val fVal = data.lastData.toFloat()
        mClock.speedTo(fVal, TorqueRefresher.REFRESH_INTERVAL)

        val markerValue = when (data.display.maxMarksActive) {
            MaxControl.MAX -> data.maxValue
            MaxControl.MIN -> data.minValue
            else -> Double.NaN
        }
        val shouldShowMarker = when (data.display.maxMarksActive) {
            MaxControl.MAX -> markerValue.isFinite() && markerValue > fVal
            MaxControl.MIN -> markerValue.isFinite() && markerValue < fVal
            else -> false
        }
        if (shouldShowMarker) {
            mMax.setSpeedAt(markerValue.toFloat())
            mMax.visibility = View.VISIBLE
        } else {
            mMax.visibility = View.INVISIBLE
        }
    }
}
