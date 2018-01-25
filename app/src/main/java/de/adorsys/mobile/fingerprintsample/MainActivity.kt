package de.adorsys.mobile.fingerprintsample

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import android.widget.Toast
import de.adorsys.mobile.fingerprintlibrary.AuthenticationListener
import de.adorsys.mobile.fingerprintlibrary.FingerprintAuthenticator

class MainActivity : AppCompatActivity(), AuthenticationListener {
    private lateinit var fingerprintAuthenticator: FingerprintAuthenticator
    private lateinit var fingerprintIcon: ImageView

    private var iconFingerprintEnabled: Drawable? = null
    private var iconFingerprintError: Drawable? = null


    override fun onFingerprintAuthenticationSuccess() {
        Toast.makeText(this, R.string.message_success, Toast.LENGTH_SHORT).show()
        fingerprintAuthenticator.subscribe(this)
        fingerprintIcon.setImageDrawable(iconFingerprintEnabled)
    }

    override fun onFingerprintAuthenticationFailure(errorMessage: String, errorCode: Int) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        fingerprintIcon.setImageDrawable(iconFingerprintError)
        fingerprintAuthenticator.subscribe(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        iconFingerprintEnabled = ResourcesCompat.getDrawable(resources, R.drawable.ic_fingerprint_on, theme)
        iconFingerprintError = ResourcesCompat.getDrawable(resources, R.drawable.ic_fingerprint_off, theme)

        // You can also assign a map of error strings for the errors defined in the lib as second parameter
//        val errors = mapOf(
//                Pair<Int, String>(FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE, getString(R.string.error_override_hw_unavailable)),
//                Pair<Int, String>(FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS, getString(R.string.error_override_unable_to_process)),
//                Pair<Int, String>(FingerprintManager.FINGERPRINT_ERROR_TIMEOUT, getString(R.string.error_override_error_timeout)),
//                Pair<Int, String>(FingerprintManager.FINGERPRINT_ERROR_NO_SPACE, getString(R.string.error_override_no_space)),
//                Pair<Int, String>(FingerprintManager.FINGERPRINT_ERROR_CANCELED, getString(R.string.error_override_canceled)),
//                Pair<Int, String>(FingerprintManager.FINGERPRINT_ERROR_LOCKOUT, getString(R.string.error_override_lockout)),
//                Pair<Int, String>(FingerprintManager.FINGERPRINT_ERROR_VENDOR, getString(R.string.error_override_vendor)),
//                Pair<Int, String>(FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT, getString(R.string.error_override_lockout_permanent)),
//                Pair<Int, String>(FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED, getString(R.string.error_override_user_cancel)),
//                Pair<Int, String>(FingerprintAuthenticator.FINGERPRINT_ERROR_NOT_RECOGNIZED, getString(R.string.error_override_not_recognized)))
//        fingerprintAuthenticator = FingerprintAuthenticator(applicationContext, errors)
        fingerprintAuthenticator = FingerprintAuthenticator(applicationContext)
        fingerprintAuthenticator.subscribe(this)

        val fingerprintsEnabled = fingerprintAuthenticator.hasFingerprintEnrolled()
        fingerprintIcon = findViewById(R.id.login_fingerprint_icon)
        fingerprintIcon.setImageDrawable(if (fingerprintsEnabled) iconFingerprintEnabled else iconFingerprintError)

        if (!fingerprintsEnabled) {
            Toast.makeText(this, R.string.error_override_hw_unavailable, Toast.LENGTH_LONG).show()
        }
    }
}
