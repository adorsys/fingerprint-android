package de.adorsys.android.finger

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Handler
import android.text.TextUtils
import androidx.biometric.BiometricPrompt
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executors

/**
 * This class handles the fingerprint communication with the user's system and simplifies its API.
 *
 * @param errors You can assign the class your personal error strings for the error codes by passing
 *     a map of fingerprint error codes and their mappings
 *
 * @param useSystemErrors You can force the library to use the human readable error string returned by the system
 *     but in at least the two locked cases it is not recommended. The system error messages don't inform the user
 *     what he/she has to do and the library doesn't subscribe the authorization until the necessary condition is true again.
 *     This is explained in the library's error messages.
 */
class Finger @JvmOverloads constructor(
    context: Context,
    private val errors: Map<Int, String> = emptyMap(),
    private val useSystemErrors: Boolean = false
) {

    private val applicationContext = context.applicationContext
    private val handler = Handler()
    private var fingerprintManager: FingerprintManagerCompat? = null
    private var fingerListener: FingerListener? = null

    init {
        fingerprintManager =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                FingerprintManagerCompat.from(context)
            } else {
                null
            }
    }

    // There is no active permission request required for using the fingerprint
    // and it is declared inside the AndroidManifest
    @SuppressLint("MissingPermission")
    @TargetApi(Build.VERSION_CODES.M)
    fun hasFingerprintEnrolled(): Boolean {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && fingerprintManager?.isHardwareDetected == true
                && fingerprintManager?.hasEnrolledFingerprints() == true)
    }

    /**
     * Subscribe for the fingerprint events by passing an FingerListener and calling FingerprintManager.authenticate.
     * If the FingerprintManager is currently locked the lockoutRunnable is started instead of subscribing
     */
    // There is no active permission request required for using the fingerprint
    // and it is declared inside the AndroidManifest
    @SuppressLint("MissingPermission")
    @TargetApi(Build.VERSION_CODES.M)
    fun subscribe(listener: FingerListener) {
        fingerListener = listener
    }

    /**
     * Call unSubscribe to make sure that a listener is not notified after it should be.
     */
    fun unSubscribe() {
        fingerListener = null
    }

    private fun getErrorMessage(code: Int, errString: CharSequence?): String {
        return when (code) {
            FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_hardware_unavailable))
            FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS,
            BiometricPrompt.ERROR_UNABLE_TO_PROCESS ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_unable_to_process))
            FingerprintManager.FINGERPRINT_ERROR_TIMEOUT,
            BiometricPrompt.ERROR_TIMEOUT ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_timeout))
            FingerprintManager.FINGERPRINT_ERROR_NO_SPACE,
            BiometricPrompt.ERROR_NO_SPACE ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_no_space))
            FingerprintManager.FINGERPRINT_ERROR_CANCELED,
            BiometricPrompt.ERROR_CANCELED ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_canceled))
            FingerprintManager.FINGERPRINT_ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT ->
                if (errors.contains(code)) errors.getValue(code)
                // you should not use the string returned by the system to make sure the user
                // knows that he/she has to wait for 30 seconds
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_lockout))
            FingerprintManager.FINGERPRINT_ERROR_VENDOR,
            BiometricPrompt.ERROR_VENDOR ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_not_recognized))
            FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT ->
                if (errors.contains(code)) errors.getValue(code)
                // you should not use the string returned by the system to make sure the user
                // knows that he/she has to lock the system and return by using another pattern
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_lockout_permanent))
            FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_USER_CANCELED ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_user_cancelled))
            FINGERPRINT_ERROR_NOT_RECOGNIZED,
            BiometricPrompt.ERROR_NO_BIOMETRICS ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_not_recognized))
            else -> return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
        }
    }

    private fun determineErrorStringSource(systemErrorString: String?, libraryErrorString: String): String {
        return if (useSystemErrors && !TextUtils.isEmpty(systemErrorString)) {
            systemErrorString.toString()
        } else {
            libraryErrorString
        }
    }

    /**
     * Shows an fingerprint dialog depending on the API level.
     *
     * @param activity
     * @param strings contains the strings needed in the dialog - first: title, second: subtitle, third: message
     * @param cancelButtonText contains the text resource needed for the button -
     * the string which should be displayed on the cancel button, defaults to android.R.string.cancel
     */
    @JvmOverloads
    fun showDialog(
        activity: FragmentActivity,
        strings: Triple<String, String?, String?>,
        cancelButtonText: String? = null
    ) {
        // temporary workaround for NullPointerException in androidx.biometric library
        // See more at https://issuetracker.google.com/issues/122054485
        handler.postDelayed({
            showBiometricPrompt(activity, strings, cancelButtonText)
        }, 250)
    }

    private fun showBiometricPrompt(
        activity: FragmentActivity,
        strings: Triple<String, String?, String?>,
        cancelButtonText: String?
    ) {

        val (title, subtitle, description) = strings

        val prompt = BiometricPrompt(activity, Executors.newSingleThreadExecutor(), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                activity.runOnUiThread {
                    fingerListener?.onFingerprintAuthenticationFailure(getErrorMessage(errorCode, errString), errorCode)
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                activity.runOnUiThread {
                    fingerListener?.onFingerprintAuthenticationSuccess()
                }
            }

            override fun onAuthenticationFailed() {
                activity.runOnUiThread {
                    fingerListener?.onFingerprintAuthenticationFailure(
                        getErrorMessage(FINGERPRINT_ERROR_NOT_RECOGNIZED, null),
                        FINGERPRINT_ERROR_NOT_RECOGNIZED
                    )
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(cancelButtonText ?: activity.getString(android.R.string.cancel))
            .build()

        prompt.authenticate(promptInfo)
    }

    companion object {
        const val FINGERPRINT_ERROR_NOT_RECOGNIZED = -999
    }
}
