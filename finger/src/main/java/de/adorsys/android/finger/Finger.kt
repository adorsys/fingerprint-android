package de.adorsys.android.finger

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Handler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executors

/**
 * This class handles the fingerprint communication with the user's system and simplifies its API.
 *
 * @param context Any android context which is always mapped to the application context.
 * @param errors You can assign your personal error strings to the androidx.biometric.BiometricPrompt's
 * error codes available in BiometricConstants to display them to the user.

 * These currently are:
 * - ERROR_HW_UNAVAILABLE,
 * - ERROR_UNABLE_TO_PROCESS,
 * - ERROR_TIMEOUT,
 * - ERROR_NO_SPACE,
 * - ERROR_CANCELED,
 * - ERROR_LOCKOUT,
 * - ERROR_VENDOR,
 * - ERROR_LOCKOUT_PERMANENT,
 * - ERROR_USER_CANCELED,
 * - ERROR_NO_BIOMETRICS,
 * - ERROR_HW_NOT_PRESENT,
 * - ERROR_NEGATIVE_BUTTON,
 * - ERROR_NO_DEVICE_CREDENTIAL
 *
 */
class Finger @JvmOverloads constructor(context: Context, private val errors: Map<Int, String> = emptyMap()) {

    private val applicationContext = context.applicationContext
    private val handler = Handler()
    private var fingerListener: FingerListener? = null

    // lazy initialization as maybe never assigned
    private val biometricManager: BiometricManager? by lazy {
        BiometricManager.from(context)
    }

    /**
     * Check if the device has suitable hardware for fingerprint authentication
     * and if the device has setup fingerprints to check on.
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun hasFingerprintEnrolled() = when (biometricManager?.canAuthenticate()) {
        BIOMETRIC_SUCCESS -> true
        else -> false
    }

    /**
     * Subscribe for the fingerprint events by passing an [FingerListener].
     * Best place to to this is onResume.
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun subscribe(listener: FingerListener) {
        fingerListener = listener
    }

    /**
     * Call unSubscribe to make sure that a listener is not notified after it should be.
     * Best place to to this is onPause.
     */
    fun unSubscribe() {
        fingerListener = null
    }

    /**
     * Shows a biometric prompt and propagates the result of the dialog to [FingerListener].
     *
     * @param activity
     * @param strings contains the strings needed in the dialog - title, subtitle, message, cancel button text
     */
    fun showDialog(
        activity: FragmentActivity,
        strings: DialogStrings
    ) {
        // temporary workaround for NullPointerException in androidx.biometric library
        // See more at https://issuetracker.google.com/issues/122054485
        handler.postDelayed({
            showBiometricPrompt(activity, strings)
        }, 250)
    }

    private fun showBiometricPrompt(
        activity: FragmentActivity,
        strings: DialogStrings
    ) {

        val (title, subtitle, description, cancelButtonText) = strings

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
                        getErrorMessage(ERROR_NOT_RECOGNIZED, null),
                        ERROR_NOT_RECOGNIZED
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

    private fun getErrorMessage(code: Int, errString: CharSequence?): String {
        return when (code) {
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE ->
                if (errors.contains(code)) errors.getValue(code)
                else return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
            BiometricPrompt.ERROR_UNABLE_TO_PROCESS ->
                if (errors.contains(code)) errors.getValue(code)
                else return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
            BiometricPrompt.ERROR_TIMEOUT ->
                if (errors.contains(code)) errors.getValue(code)
                else return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
            BiometricPrompt.ERROR_NO_SPACE ->
                if (errors.contains(code)) errors.getValue(code)
                else return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
            BiometricPrompt.ERROR_CANCELED ->
                if (errors.contains(code)) errors.getValue(code)
                else return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
            BiometricPrompt.ERROR_LOCKOUT ->
                if (errors.contains(code)) errors.getValue(code)
                // you should not use the string returned by the system to make sure the user
                // knows that he/she has to wait for 30 seconds
                else return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
            BiometricPrompt.ERROR_VENDOR ->
                if (errors.contains(code)) errors.getValue(code)
                else return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                if (errors.contains(code)) errors.getValue(code)
                // you should not use the string returned by the system to make sure the user
                // knows that he/she has to lock the system and return by using another pattern
                else return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
            BiometricPrompt.ERROR_USER_CANCELED ->
                if (errors.contains(code)) errors.getValue(code)
                else return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
            ERROR_NOT_RECOGNIZED,
            BiometricPrompt.ERROR_NO_BIOMETRICS ->
                if (errors.contains(code)) errors.getValue(code)
                else return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
            else -> return errString?.toString() ?: applicationContext.getString(R.string.fingerprint_error_unknown)
        }
    }

    companion object {
        const val ERROR_NOT_RECOGNIZED = -999
    }

    data class DialogStrings @JvmOverloads constructor(
        @JvmField val title: String,
        @JvmField val subTitle: String? = null,
        @JvmField val description: String? = null,
        @JvmField val cancelButtonText: String? = null
    )
}
