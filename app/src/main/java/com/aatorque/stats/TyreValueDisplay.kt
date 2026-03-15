package com.aatorque.stats

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aatorque.prefs.SettingsViewModel
import com.aatorque.stats.databinding.FragmentTyreValueBinding
import timber.log.Timber

class TyreValueDisplay : Fragment() {

    private lateinit var binding: FragmentTyreValueBinding
    private var unit = ""
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
        Timber.i("onCreateView")
        binding = FragmentTyreValueBinding.inflate(inflater, container, false)
        val alignRight = id in RIGHT_ALIGNED_IDS
        binding.valueElement.textAlignment = if (alignRight) {
            View.TEXT_ALIGNMENT_VIEW_END
        } else {
            View.TEXT_ALIGNMENT_VIEW_START
        }
        binding.valueElement.gravity = if (alignRight) Gravity.END else Gravity.START
        if (id in GR_DASH_IDS) {
            binding.valueElement.layoutParams = binding.valueElement.layoutParams.apply {
                width = WRAP_CONTENT
            }
            binding.valueElement.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            binding.secondaryFont = ResourcesCompat.getFont(requireContext(), R.font.open_sans)
        } else {
            settingsViewModel.secondaryTypefaceLiveData.observe(viewLifecycleOwner, this::setupTypeface)
        }
        return binding.root
    }

    fun setupElement(data: TorqueData) {
        unit = data.display.unit
        binding.visible = data.pid != null
        binding.value = "-"
        data.notifyUpdate = this::onUpdate
    }

    private fun setupTypeface(typeface: Typeface) {
        binding.secondaryFont = typeface
    }

    @SuppressLint("SetTextI18n")
    fun onUpdate(data: TorqueData) {
        binding.value = if (unit.isBlank()) data.lastDataStr else "${data.lastDataStr} $unit"
    }

    companion object {
        private val RIGHT_ALIGNED_IDS = setOf(
            R.id.display3,
            R.id.display4,
            R.id.display7,
            R.id.display8,
            R.id.grDisplay3,
            R.id.grDisplay4,
            R.id.grDisplay7,
            R.id.grDisplay8,
        )

        private val GR_DASH_IDS = setOf(
            R.id.grDisplay1,
            R.id.grDisplay2,
            R.id.grDisplay3,
            R.id.grDisplay4,
            R.id.grDisplay5,
            R.id.grDisplay6,
            R.id.grDisplay7,
            R.id.grDisplay8,
        )
    }
}
