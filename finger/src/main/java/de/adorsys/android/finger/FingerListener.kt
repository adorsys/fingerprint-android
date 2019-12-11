package de.adorsys.android.finger

/**
 * Interface getting success or failure result from [Finger]
 */
interface FingerListener {

    /**
     * Called after successful authentication.
     */
    fun onFingerprintAuthenticationSuccess()

    /**
     * Called after an error or authentication failure.
     *
     * @param errorMessage An informative string given by the underlying fingerprint sdk that can be
     * displayed in the ui. This string is never null, and will be localized to the
     * current locale. You should show this text to the user, or some other message of
     * your own based on the failureReason.
     * @param errorCode    The specific error code returned by the module's underlying sdk. Check the
     * constants defined in the module for possible values and their meanings.
     */
    fun onFingerprintAuthenticationFailure(errorMessage: String, errorCode: Int)
}
