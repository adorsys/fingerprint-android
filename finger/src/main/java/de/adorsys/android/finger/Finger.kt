package de.adorsys.android.finger

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.text.TextUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.google.android.material.bottomsheet.BottomSheetDialog

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
    private val lockoutRunnable = Runnable {
        lockoutOccurred = false
        fingerListener?.onFingerprintLockoutReleased()
    }
    private var fingerprintManager: FingerprintManagerCompat? = null
    private var fingerListener: FingerListener? = null
    private var lockoutOccurred = false

    private var fingerprintDialog: BottomSheetDialog? = null

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
            BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT,
            BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_hardware_unavailable))
            FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS,
            BiometricPrompt.BIOMETRIC_ERROR_UNABLE_TO_PROCESS ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_unable_to_process))
            FingerprintManager.FINGERPRINT_ERROR_TIMEOUT,
            BiometricPrompt.BIOMETRIC_ERROR_TIMEOUT ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_timeout))
            FingerprintManager.FINGERPRINT_ERROR_NO_SPACE,
            BiometricPrompt.BIOMETRIC_ERROR_NO_SPACE ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_no_space))
            FingerprintManager.FINGERPRINT_ERROR_CANCELED,
            BiometricPrompt.BIOMETRIC_ERROR_CANCELED ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_canceled))
            FingerprintManager.FINGERPRINT_ERROR_LOCKOUT,
            BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT ->
                if (errors.contains(code)) errors.getValue(code)
                // you should not use the string returned by the system to make sure the user
                // knows that he/she has to wait for 30 seconds
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_lockout))
            FingerprintManager.FINGERPRINT_ERROR_VENDOR,
            BiometricPrompt.BIOMETRIC_ERROR_VENDOR ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_not_recognized))
            FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT ->
                if (errors.contains(code)) errors.getValue(code)
                // you should not use the string returned by the system to make sure the user
                // knows that he/she has to lock the system and return by using another pattern
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_lockout_permanent))
            FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED,
            BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ->
                if (errors.contains(code)) errors.getValue(code)
                else return determineErrorStringSource(errString?.toString(), applicationContext.getString(R.string.fingerprint_error_user_cancelled))
            FINGERPRINT_ERROR_NOT_RECOGNIZED,
            BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS ->
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
     * @param context when targeting api level < 29 the FingerprintDialog has to be build manually and a context is needed for that.
     * @param strings contains the strings needed in the dialog - first: title, second: subtitle, third: message
     * @param buttonDefinition contains the resources need for the button -
     * first: the string which should be displayed on the cancel button, defaults to android.R.string.cancel,
     * second: the action which should be invoked when clicking on the button - can be null in which case nothing is done
     * @param drawableRes when targeting api level < 29 the FingerprintDialog has to be build manually. drawable definitions have to be provided.
     * The drawableRes contains the drawable res definitions for the dialog's icons - first: app icon, second: fingerprintIcon.
     * The fingerprint icon can be null in which case the default icon is taken
     * @param dialogLayoutRes when targeting api level < 29 the FingerprintDialog has to be build manually.
     * A layout can be specified which overrides the default one. Please make sure you use the same ids though:
     * app_icon / title_text_view / subtitle_text_view / fingerprint_icon / message_text_view / cancel_button
     */
    fun showDialog(
        context: Context,
        strings: Triple<String, String, String>,
        buttonDefinition: Pair<String?, (() -> Unit)?>,
        drawableRes: Pair<Int, Int?> = Pair(R.drawable.rounded_blue_background, null),
        @LayoutRes dialogLayoutRes: Int = R.layout.dialog_fingerprint_authentication
    ) {
        if (hasBioMetricPromptAvailable()) {
            showBiometricPrompt(strings, buttonDefinition)
        } else {
            showFingerprintManagerDialog(context, strings, drawableRes, buttonDefinition, dialogLayoutRes)
        }
    }

    private fun showFingerprintManagerDialog(
        context: Context,
        texts: Triple<String, String, String>,
        drawableRes: Pair<Int, Int?>,
        buttonDefinition: Pair<String?, (() -> Unit)?>,
        @LayoutRes dialogLayoutRes: Int
    ) {

        val (title, subtitle, message) = texts
        val (appIcon, fingerprintIcon) = drawableRes
        val (cancelButtonText, onNegativeButtonAction) = buttonDefinition

        if (fingerprintDialog != null && fingerprintDialog?.isShowing != true) {
            fingerprintDialog = BottomSheetDialog(context)
            fingerprintDialog?.setContentView(dialogLayoutRes)
            fingerprintDialog?.setCancelable(true)
            fingerprintDialog?.show()

            val appIconImageView = fingerprintDialog?.findViewById<ImageView>(R.id.app_icon)
            val titleTextView = fingerprintDialog?.findViewById<TextView>(R.id.title_text_view)
            val subtitleTextView = fingerprintDialog?.findViewById<TextView>(R.id.subtitle_text_view)
            val fingerprintIconImageView = fingerprintDialog?.findViewById<ImageView>(R.id.fingerprint_icon)
            val messageTextView = fingerprintDialog?.findViewById<TextView>(R.id.message_text_view)
            val cancelButton = fingerprintDialog?.findViewById<Button>(R.id.cancel_button)

            titleTextView?.text = title
            subtitleTextView?.text = subtitle
            messageTextView?.text = message
            appIconImageView?.setImageDrawable(ContextCompat.getDrawable(applicationContext, appIcon))
            fingerprintIcon?.let { fingerprintIconImageView?.setImageDrawable(ContextCompat.getDrawable(applicationContext, it)) }

            cancelButton?.text = cancelButtonText ?: applicationContext.getString(android.R.string.cancel)

            cancelButton?.setOnClickListener {
                unSubscribe()
                onNegativeButtonAction?.invoke()
                fingerprintDialog?.dismiss()
            }
        }

        val cancellationSignal =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                androidx.core.os.CancellationSignal()
            } else {
                null
            }
        fingerprintManager?.authenticate(null, 0, cancellationSignal, object : FingerprintManagerCompat.AuthenticationCallback() {
            /**
             * Called when an unrecoverable error has been encountered and the operation is complete.
             * No further callbacks will be made on this object.
             *
             * @param errorCode An integer identifying the error message
             * @param errString A human-readable error string that can be shown in UI
             */
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                // workaround for FINGERPRINT_ERROR_CANCELED / Android bug: https://stackoverflow.com/a/40854259/3734116
                // lockoutOccurred has to be checked here and set to true after sending error message
                // to make sure the error message is send once but not twice
                if (errorCode == FingerprintManager.FINGERPRINT_ERROR_CANCELED || lockoutOccurred) {
                    return
                }
                if (errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT && !lockoutOccurred) {
                    lockoutOccurred = true
                    handler.postDelayed(lockoutRunnable, FINGERPRINT_LOCKOUT_TIME)
                }
                fingerListener?.onFingerprintAuthenticationFailure(getErrorMessage(errorCode, errString), errorCode)
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
                fingerListener?.onFingerprintAuthenticationFailure(getErrorMessage(helpCode, helpString), helpCode)
            }

            /**
             * Called when a fingerprint is recognized.
             *
             * @param result An object containing authentication-related data
             */
            override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                fingerListener?.onFingerprintAuthenticationSuccess()
            }

            /**
             * Called when a fingerprint is not recognized.
             * The user probably used the wrong finger.
             */
            override fun onAuthenticationFailed() {
                fingerListener?.onFingerprintAuthenticationFailure(getErrorMessage(FINGERPRINT_ERROR_NOT_RECOGNIZED, null), FINGERPRINT_ERROR_NOT_RECOGNIZED)
            }
        }, null)
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun showBiometricPrompt(
        strings: Triple<String, String, String>,
        buttonDefinition: Pair<String?, (() -> Unit)?>
    ) {

        val (title, subtitle, description) = strings
        val (cancelButtonText, onNegativeButtonAction) = buttonDefinition

        val prompt = BiometricPrompt.Builder(applicationContext)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButton(
                cancelButtonText ?: applicationContext.getString(android.R.string.cancel),
                applicationContext.mainExecutor,
                DialogInterface.OnClickListener { _, _ -> onNegativeButtonAction?.invoke() })
            .build()

        val cancellationSignal = CancellationSignal()
        prompt.authenticate(cancellationSignal, applicationContext.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                fingerListener?.onFingerprintAuthenticationFailure(getErrorMessage(errorCode, errString), errorCode)
            }

            override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                fingerListener?.onFingerprintAuthenticationFailure(getErrorMessage(helpCode, helpString), helpCode)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                fingerListener?.onFingerprintAuthenticationSuccess()
            }

            override fun onAuthenticationFailed() {
                fingerListener?.onFingerprintAuthenticationFailure(getErrorMessage(FINGERPRINT_ERROR_NOT_RECOGNIZED, null), FINGERPRINT_ERROR_NOT_RECOGNIZED)
            }
        })
    }

    private fun hasBioMetricPromptAvailable(): Boolean {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
    }

    companion object {
        // Use a bit more than 30 seconds to prevent subscribing too early
        private const val FINGERPRINT_LOCKOUT_TIME = 30000L
        const val FINGERPRINT_ERROR_NOT_RECOGNIZED = -999
    }
}
