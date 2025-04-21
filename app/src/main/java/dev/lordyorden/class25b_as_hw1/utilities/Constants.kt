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

    object OTP {
        const val OTP_LENGTH = 6
        const val OTP_SECRET = "test"
        const val AUTO_GENERATE_SECRET = true //false for better testing
    }

}