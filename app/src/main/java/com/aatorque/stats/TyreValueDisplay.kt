package com.aatorque.stats

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        settingsViewModel.typefaceLiveData.observe(viewLifecycleOwner, this::setupTypeface)
        return binding.root
    }

    fun setupElement(data: TorqueData) {
        unit = data.display.unit
        binding.visible = data.pid != null
        binding.value = "-"
        data.notifyUpdate = this::onUpdate
    }

    private fun setupTypeface(typeface: Typeface) {
        binding.font = typeface
    }

    @SuppressLint("SetTextI18n")
    fun onUpdate(data: TorqueData) {
        binding.value = if (unit.isBlank()) data.lastDataStr else "${data.lastDataStr} $unit"
    }
}
