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
        fingerprintAuthenticator.register(this)
        fingerprintIcon.setImageDrawable(iconFingerprintEnabled)
    }

    override fun onFingerprintAuthenticationFailure(errorMessage: String, errorCode: Int) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        fingerprintAuthenticator.register(this)
        fingerprintIcon.setImageDrawable(iconFingerprintError)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        iconFingerprintEnabled = ResourcesCompat.getDrawable(resources, R.drawable.ic_fingerprint_on, theme)
        iconFingerprintError = ResourcesCompat.getDrawable(resources, R.drawable.ic_fingerprint_off, theme)

        fingerprintAuthenticator = FingerprintAuthenticator(applicationContext)
        fingerprintAuthenticator.register(this)

        val fingerprintsEnabled = fingerprintAuthenticator.hasFingerprintEnrolled()
        fingerprintIcon = findViewById(R.id.login_fingerprint_icon)
        fingerprintIcon.setImageDrawable(if (fingerprintsEnabled) iconFingerprintEnabled else iconFingerprintError)
    }
}
