package dev.lordyorden.class25b_as_hw1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import dev.lordyorden.class25b_as_hw1.utilities.Constants

class MyReceiver : BroadcastReceiver() {

//    private val pipVm: SwitchViewModel by lazy {
//        SwitchViewModel()
//    }

    // Called when an item is clicked.
    override fun onReceive(context: Context?, intent: Intent?) {
//        if (intent == null || intent.action != Constants.Pip.ACTION_PIP) {
//            return
//        }
        when (intent?.getIntExtra(Constants.Pip.EXTRA_CONTROL_STATE, 0)) {
            Constants.Pip.CONTROL_STATE_ON -> {
                Toast.makeText(context, "testing broadcast on", Toast.LENGTH_SHORT).show()
                //callback?.onPipSwitchOn(context)
            }
            Constants.Pip.CONTROL_STATE_OFF -> {
                Toast.makeText(context, "testing broadcast off", Toast.LENGTH_SHORT).show()
                //callback?.onPipSwitchOff(context)
            }
            else -> {
                Toast.makeText(context, "testing broadcast error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}