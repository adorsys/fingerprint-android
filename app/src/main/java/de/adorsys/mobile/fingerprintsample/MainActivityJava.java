package de.adorsys.mobile.fingerprintsample;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Collections;

import de.adorsys.mobile.fingerprintlibrary.AuthenticationListener;
import de.adorsys.mobile.fingerprintlibrary.FingerprintAuthenticator;

@SuppressLint("Registered")
public class MainActivityJava extends AppCompatActivity implements AuthenticationListener {
    private FingerprintAuthenticator fingerprintAuthenticator;
    private ImageView fingerprintIcon;

    private Drawable iconFingerprintEnabled;
    private Drawable iconFingerprintError;


    @Override
    public void onFingerprintAuthenticationSuccess() {
        Toast.makeText(this, R.string.message_success, Toast.LENGTH_SHORT).show();
        fingerprintAuthenticator.subscribe(this);
        fingerprintIcon.setImageDrawable(iconFingerprintEnabled);
    }

    @Override
    public void onFingerprintAuthenticationFailure(@NonNull String errorMessage, int errorCode) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        fingerprintIcon.setImageDrawable(iconFingerprintError);
        fingerprintAuthenticator.subscribe(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        fingerprintAuthenticator.subscribe(this);
        boolean fingerprintsEnabled = fingerprintAuthenticator.hasFingerprintEnrolled();
        fingerprintIcon = findViewById(R.id.login_fingerprint_icon);
        fingerprintIcon.setImageDrawable(fingerprintsEnabled ? iconFingerprintEnabled : iconFingerprintError);

        if (!fingerprintsEnabled) {
            Toast.makeText(this, R.string.error_override_hw_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iconFingerprintEnabled = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_fingerprint_on, getTheme());
        iconFingerprintError = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_fingerprint_off, getTheme());

        // You can also assign a map of error strings for the errors defined in the lib as second parameter
        fingerprintAuthenticator = new FingerprintAuthenticator(getApplicationContext(), Collections.<Integer, String>emptyMap());
    }
}
