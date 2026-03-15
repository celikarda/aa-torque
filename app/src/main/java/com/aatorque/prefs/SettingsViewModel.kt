package com.aatorque.prefs

import android.app.Application
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val selectedMainFont = MutableLiveData<Int>()
    private val selectedSecondaryFont = MutableLiveData<Int>()
    val chartVisible = MutableLiveData<Boolean>()
    val minMaxBelow = MutableLiveData<Boolean>()

    val mainTypefaceLiveData = selectedMainFont.map {
        return@map ResourcesCompat.getFont(getApplication(), it)!!
    }

    val secondaryTypefaceLiveData = selectedSecondaryFont.map {
        return@map ResourcesCompat.getFont(getApplication(), it)!!
    }

    fun setMainFont(@FontRes font: Int) {
        selectedMainFont.value = font
    }

    fun setSecondaryFont(@FontRes font: Int) {
        selectedSecondaryFont.value = font
    }
}
