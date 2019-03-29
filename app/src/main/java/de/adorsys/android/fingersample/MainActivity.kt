package de.adorsys.android.fingersample

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import de.adorsys.android.finger.Finger
import de.adorsys.android.finger.FingerListener

class MainActivity : AppCompatActivity(), FingerListener {
    private lateinit var finger: Finger
    private lateinit var fingerprintIcon: ImageView

    private var iconFingerprintEnabled: Drawable? = null
    private var iconFingerprintError: Drawable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        finger = Finger(applicationContext)
    }

    override fun onResume() {
        super.onResume()

        iconFingerprintEnabled = ResourcesCompat.getDrawable(resources, R.drawable.ic_fingerprint_on, theme)
        iconFingerprintError = ResourcesCompat.getDrawable(resources, R.drawable.ic_fingerprint_off, theme)

        finger.subscribe(this)

        val fingerprintsEnabled = finger.hasFingerprintEnrolled()

        fingerprintIcon = findViewById(R.id.login_fingerprint_icon)
        fingerprintIcon.setImageDrawable(if (fingerprintsEnabled) iconFingerprintEnabled else iconFingerprintError)

        if (!fingerprintsEnabled) {
            Toast.makeText(this, R.string.error_override_hw_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        finger.unSubscribe()
    }

    override fun onFingerprintAuthenticationSuccess() {
        Toast.makeText(this, R.string.message_success, Toast.LENGTH_SHORT).show()
        fingerprintIcon.setImageDrawable(iconFingerprintEnabled)
        finger.subscribe(this)
    }

    override fun onFingerprintAuthenticationFailure(errorMessage: String, errorCode: Int) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        fingerprintIcon.setImageDrawable(iconFingerprintError)
        finger.subscribe(this)
    }

    override fun onFingerprintLockoutReleased() {
        fingerprintIcon.setImageDrawable(iconFingerprintEnabled)
        finger.subscribe(this)
    }
}
