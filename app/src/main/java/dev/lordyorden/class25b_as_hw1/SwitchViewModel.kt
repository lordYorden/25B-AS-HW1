package dev.lordyorden.class25b_as_hw1

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SwitchViewModel: ViewModel() {

    private val _isPipSwitchOn = MutableLiveData<Boolean>().apply {
        value = false
    }

    val isPipSwitchOn: LiveData<Boolean> = _isPipSwitchOn

    fun setPipSwitchOn(isOn: Boolean) {
        _isPipSwitchOn.postValue(isOn)
    }
}