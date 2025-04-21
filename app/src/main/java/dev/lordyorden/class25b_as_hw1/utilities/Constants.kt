package dev.lordyorden.class25b_as_hw1.utilities

class Constants {

    object STEPS {
        const val STEP_THRESHOLD: Long = 3000L
    }

    object RotationLock{
        const val DEFAULT_ACCELEROMETER_ROTATION: Int = 0
    }

    object AppExist{
        const val NAME :String = "com.whatsapp"
    }

    object Pip{
        const val ACTION_PIP = "dev.lordyorden.class25b_as_hw1.action.PIP"
        const val EXTRA_CONTROL_STATE = "switch_state"
        const val CONTROL_STATE_ON = 1
        const val CONTROL_STATE_OFF = 0
    }

}