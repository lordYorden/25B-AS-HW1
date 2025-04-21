package dev.lordyorden.class25b_as_hw1

import android.content.Context

interface BroadcastReceiverCallback {
    fun onPipSwitchOn(context: Context?)
    fun onPipSwitchOff(context: Context?)
}