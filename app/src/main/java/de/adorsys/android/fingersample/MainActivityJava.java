package de.adorsys.android.fingersample;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import de.adorsys.android.finger.Finger;
import de.adorsys.android.finger.FingerListener;

@SuppressLint("Registered") // Only exits for java documentation purposes
public class MainActivityJava extends AppCompatActivity implements FingerListener {
    private Finger finger;
    private ImageView fingerprintIcon;

    private Drawable iconFingerprintEnabled;
    private Drawable iconFingerprintError;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iconFingerprintEnabled = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_fingerprint_on, getTheme());
        iconFingerprintError = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_fingerprint_off, getTheme());

        // You can also assign a map of error strings for the errors defined in the lib as second parameter
        finger = new Finger(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        finger.subscribe(this);
        boolean fingerprintsEnabled = finger.hasFingerprintEnrolled();

        fingerprintIcon = findViewById(R.id.login_fingerprint_icon);
        fingerprintIcon.setImageDrawable(fingerprintsEnabled ? iconFingerprintEnabled : iconFingerprintError);

        Button showDialogButton = findViewById(R.id.show_dialog_button);
        showDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        if (!fingerprintsEnabled) {
            Toast.makeText(this, R.string.error_override_hw_unavailable, Toast.LENGTH_LONG).show();
        } else {
            showDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finger.unSubscribe();
    }

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

    private void showDialog() {
        finger.showDialog(
                this,
                new Finger.DialogStrings(
                        getString(R.string.text_fingerprint)
                )
        );
    }
}
