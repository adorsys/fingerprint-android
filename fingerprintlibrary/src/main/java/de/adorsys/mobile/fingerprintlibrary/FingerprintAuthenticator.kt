package de.adorsys.mobile.fingerprintlibrary

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat

@TargetApi(Build.VERSION_CODES.M)
class FingerprintAuthenticator(private val context: Context, private val errors: Map<Int, String> = emptyMap()) : FingerprintManagerCompat.AuthenticationCallback() {
    private var fingerprintManager: FingerprintManagerCompat? = null
    private var authenticationListener: AuthenticationListener? = null

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
    fun hasFingerprintEnrolled(): Boolean {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && fingerprintManager != null
                && fingerprintManager!!.isHardwareDetected
                && fingerprintManager!!.hasEnrolledFingerprints())
    }

    // There is no active permission request required for using the fingerprint
    // and it is declared inside the AndroidManifest
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.M)
    fun register(listener: AuthenticationListener) {
        authenticationListener = listener

        if (hasFingerprintEnrolled()) {
            fingerprintManager?.authenticate(null, 0, null, this, null)
        }
    }

    /**
     * Called when an unrecoverable error has been encountered and the operation is complete.
     * No further callbacks will be made on this object.
     *
     * @param errorCode An integer identifying the error message
     * @param errString A human-readable error string that can be shown in UI
     */
    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        if (errorCode != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
            authenticationListener?.onFingerprintAuthenticationFailure(getErrorMessage(errorCode), errorCode)
        }
    }

    /**
     * Called when a recoverable error has been encountered during authentication. The help
     * string is provided to give the user guidance for what went wrong, such as
     * "Sensor dirty, please clean it."
     *
     * @param helpCode   An integer identifying the error message
     * @param helpString A human-readable string that can be shown in UI
     */
    override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
        authenticationListener?.onFingerprintAuthenticationFailure(getErrorMessage(helpCode), helpCode)
    }

    /**
     * Called when a fingerprint is recognized.
     *
     * @param result An object containing authentication-related data
     */
    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult) {
        authenticationListener?.onFingerprintAuthenticationSuccess()
    }

    /**
     * Called when a fingerprint is valid but not recognized.
     */
    override fun onAuthenticationFailed() {
        authenticationListener?.onFingerprintAuthenticationFailure(getErrorMessage(FINGERPRINT_GENERAL_ERROR), FINGERPRINT_GENERAL_ERROR)
    }

    private fun getErrorMessage(code: Int): String {
        return when (code) {
            FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE -> 
                if (errors.contains(code)) errors[code]!! else context.getString(R.string.fingerprint_error_hardware_unavailable)
            FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS ->
                if (errors.contains(code)) errors[code]!! else context.getString(R.string.fingerprint_error_unable_to_process)
            FingerprintManager.FINGERPRINT_ERROR_TIMEOUT ->
                if (errors.contains(code)) errors[code]!! else context.getString(R.string.fingerprint_error_timeout)
            FingerprintManager.FINGERPRINT_ERROR_CANCELED ->
                if (errors.contains(code)) errors[code]!! else context.getString(R.string.fingerprint_error_canceled)
            FingerprintManager.FINGERPRINT_ERROR_LOCKOUT ->
                if (errors.contains(code)) errors[code]!! else context.getString(R.string.fingerprint_error_lockout)
            FINGERPRINT_GENERAL_ERROR ->
                if (errors.contains(code)) errors[code]!! else context.getString(R.string.fingerprint_error_general)
            else -> context.getString(R.string.fingerprint_error_unknown)
        }
    }

    companion object {
        private const val FINGERPRINT_GENERAL_ERROR = -999
    }
}
