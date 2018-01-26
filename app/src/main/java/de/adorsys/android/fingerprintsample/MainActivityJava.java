package de.adorsys.android.fingerprintsample;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Collections;

import de.adorsys.android.finger.FingerListener;
import de.adorsys.android.finger.Finger;

@SuppressLint("Registered") // Only exits for java documentation purposes
public class MainActivityJava extends AppCompatActivity implements FingerListener {
    private Finger finger;
    private ImageView fingerprintIcon;

    private Drawable iconFingerprintEnabled;
    private Drawable iconFingerprintError;


    @Override
    public void onFingerprintAuthenticationSuccess() {
        Toast.makeText(this, R.string.message_success, Toast.LENGTH_SHORT).show();
        fingerprintIcon.setImageDrawable(iconFingerprintEnabled);
        finger.subscribe(this);
    }

    @Override
    public void onFingerprintAuthenticationFailure(@NonNull String errorMessage, int errorCode) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        fingerprintIcon.setImageDrawable(iconFingerprintError);
        finger.subscribe(this);
    }

    @Override
    public void onFingerprintLockoutReleased() {
        fingerprintIcon.setImageDrawable(iconFingerprintEnabled);
        finger.subscribe(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        finger.subscribe(this);
        boolean fingerprintsEnabled = finger.hasFingerprintEnrolled();
        fingerprintIcon = findViewById(R.id.login_fingerprint_icon);
        fingerprintIcon.setImageDrawable(fingerprintsEnabled ? iconFingerprintEnabled : iconFingerprintError);

        if (!fingerprintsEnabled) {
            Toast.makeText(this, R.string.error_override_hw_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finger.unSubscribe();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iconFingerprintEnabled = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_fingerprint_on, getTheme());
        iconFingerprintError = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_fingerprint_off, getTheme());

        // You can also assign a map of error strings for the errors defined in the lib as second parameter
        finger = new Finger(getApplicationContext(), Collections.<Integer, String>emptyMap(), false);
    }
}
