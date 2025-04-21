package dev.lordyorden.class25b_as_hw1

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import dev.lordyorden.class25b_as_hw1.databinding.ActivityMainBinding
import dev.lordyorden.class25b_as_hw1.utilities.Constants
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.HmacOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.HmacOneTimePasswordGenerator
import java.util.UUID


class MainActivity() : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val sensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }
    private val cameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private lateinit var stepListener: SensorEventListener
    private var stepCount: Long = 0L

    private lateinit var passGen: HmacOneTimePasswordGenerator
    private var counter: Long = 0L
    private var isPip = false

    private val isDoneAuth
        get() = binding.stepsMs.isChecked and binding.bioMs.isChecked and binding.flashlightMs.isChecked and binding.rotationMs.isChecked and binding.hasAppMs.isChecked and binding.pipOtpMs.isChecked

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    private val signal: CancellationSignal = CancellationSignal()

    private val prompt = BiometricPrompt.Builder(this)
        .setTitle("Biometric Test")
        .setSubtitle("Put your finger or enter password")
        .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        .build()

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(stepListener)
    }

    override fun onResume() {
        super.onResume()

        val pip = intent.getBooleanExtra("isPip", false)
        if (pip)
            Toast.makeText(this, "auth pip", Toast.LENGTH_SHORT).show()

        if (isPip) {
            binding.lblTitle.visibility = View.VISIBLE
            isPip = false
        }
        registerStepListener()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            binding.llcSwitches.visibility = View.GONE
            isPip = true
            binding.lblTitle.text = generateNewPinCode()
        } else {
            binding.llcSwitches.visibility = View.VISIBLE
            isPip = false
            binding.lblTitle.text = getString(R.string.auth)
        }
    }

    private fun generateNewPinCode(): String {
        return passGen.generate(counter++)
    }

    private fun configOtp(){
        val config = HmacOneTimePasswordConfig(codeDigits = Constants.OTP.OTP_LENGTH,
            hmacAlgorithm = HmacAlgorithm.SHA1)

        val secret = if (Constants.OTP.AUTO_GENERATE_SECRET) {
            UUID.randomUUID().toString()
        } else {
            Constants.OTP.OTP_SECRET
        }

        passGen = HmacOneTimePasswordGenerator(secret.toByteArray(), config)
        counter = 0
    }

    private fun initViews() {

        setupAutoPipOnExit()
        configOtp()

        binding.bioMs.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                startBioAuth()
        }

        binding.flashlightMs.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkFlashlight()
            }
        }

        binding.stepsMs.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkStepCount()
            }
        }

        binding.rotationMs.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkRotationLock()
            }
        }

        binding.hasAppMs.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkHasApp()
            }
        }

        binding.pipOtpMs.setOnCheckedChangeListener{ _, ischecked ->
            if (ischecked){
                getAndCheckOtp()
            }
        }
    }

    private fun getAndCheckOtp() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Enter OTP")
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton("OK") { dialog, _ ->
            val otp = input.text.toString()
            if (passGen.isValid(otp, counter - 1)) {
                Toast.makeText(this, "OTP is correct! Auth successful!", Toast.LENGTH_SHORT).show()
                checkDone()
            } else {
                Toast.makeText(this, "OTP is incorrect! Try again!", Toast.LENGTH_SHORT).show()
                switchOff()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            Toast.makeText(this, "OTP is canceled!", Toast.LENGTH_SHORT).show()
            switchOff()
            dialog.cancel()
        }
        builder.show()

    }

    private fun checkHasApp(packageName: String = Constants.AppExist.NAME) {

        if (isAppInstalled(packageName)){
            Toast.makeText(this, "$packageName is installed! Auth successful!", Toast.LENGTH_SHORT).show()
            checkDone()
        } else {
            Toast.makeText(this, "$packageName is not installed. Try again!", Toast.LENGTH_SHORT).show()
            switchOff()
        }
    }

    private fun checkRotationLock() {
        val locked: Int = Settings.System.getInt(
            contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            Constants.RotationLock.DEFAULT_ACCELEROMETER_ROTATION
        )

        if (locked == 1){
            Toast.makeText(this, "Lock phone rotation and try again!", Toast.LENGTH_SHORT).show()
            switchOff()
        }else{
            Toast.makeText(this, "Phone rotation is locked! Auth successful!", Toast.LENGTH_SHORT).show()
            checkDone()
        }
    }

    private fun registerStepListener() {
        val listener: SensorEventListener by lazy {
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null) return

                    stepCount = event.values[0].toLong()
                    Log.d("Steps", "Steps since last reboot: $stepCount")
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    Log.d("Steps", "Accuracy changed to: $accuracy")
                }
            }
        }

        stepListener = listener

        val supportedAndEnabled = sensorManager.registerListener(listener,
            sensor, SensorManager.SENSOR_DELAY_UI)

        Log.d("Steps", "Sensor listener registered: $supportedAndEnabled")
    }

    private fun checkStepCount() {
        sensor?.let {
            if(stepCount >= Constants.STEPS.STEP_THRESHOLD) {
                Toast.makeText(this, "Auth successful you've reached today's step goal!", Toast.LENGTH_SHORT).show()
                checkDone()
            }else{
                Toast.makeText(this, "Missing ${calcMissingSteps()} steps for today!", Toast.LENGTH_SHORT).show()
                switchOff()
            }
        } ?: {
            Toast.makeText(this, "No Steps! No login!", Toast.LENGTH_SHORT).show()
            switchOff()
        }
    }

    private fun checkDone() {
        if(isDoneAuth){
            Toast.makeText(this, "Auth Complete moving on!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun calcMissingSteps(): Long {
        return Constants.STEPS.STEP_THRESHOLD-stepCount
    }

    private fun checkFlashlight() {
        val hasFlash = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        if (hasFlash) {
            val torch = cameraManager.cameraIdList[0]
            val characteristics = cameraManager.getCameraCharacteristics(torch)
            val strength = cameraManager.getTorchStrengthLevel(torch)
            val off: Int =
                characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) ?: 0
            when (strength) {
                off -> {
                    Toast.makeText(
                        this,
                        "Turn on the flashlight and try again!",
                        Toast.LENGTH_SHORT
                    ).show()
                    switchOff()
                }

                else -> {
                    Toast.makeText(this, "Flashlight is on auth successful!", Toast.LENGTH_SHORT).show()
                    checkDone()
                }
            }
        } else {
            Toast.makeText(this, "No Flashlight, No login!", Toast.LENGTH_SHORT).show()
            switchOff()
        }


    }

    @RequiresPermission(Manifest.permission.USE_BIOMETRIC)
    private fun startBioAuth() {
        signal.setOnCancelListener {
            Log.d("auth", "canceled")
            switchOff()
        }


        prompt.authenticate(
            signal,
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(
                        applicationContext,
                        "Bio Authentication succeeded!", Toast.LENGTH_SHORT
                    ).show()
                    checkDone()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    switchOff()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT
                    )
                        .show()
                    switchOff()
                }
            })
    }

    private fun switchOff() {
        binding.bioMs.isChecked = false
        binding.flashlightMs.isChecked = false
        binding.stepsMs.isChecked= false
        binding.rotationMs.isChecked = false
        binding.hasAppMs.isChecked = false
        binding.pipOtpMs.isChecked = false
    }

    private fun isAppInstalled(packageName: String): Boolean{
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        }catch (e: PackageManager.NameNotFoundException){
            false
        }
    }

    private fun setupAutoPipOnExit(): PictureInPictureParams {
        val visibleRect = Rect()
        binding.lblTitle.getGlobalVisibleRect(visibleRect)

        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setSourceRectHint(visibleRect)
            .setSeamlessResizeEnabled(false)
            .setAutoEnterEnabled(true)
        params.setAutoEnterEnabled(true)
        return params.build().also {
            setPictureInPictureParams(it)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        minimize()
    }

    private fun minimize() {
        enterPictureInPictureMode(setupAutoPipOnExit())
    }
}