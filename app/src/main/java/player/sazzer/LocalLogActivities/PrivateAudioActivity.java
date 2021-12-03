package player.sazzer.LocalLogActivities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.Objects;
import java.util.concurrent.Executor;

import player.sazzer.R;

public class PrivateAudioActivity extends AppCompatActivity {
    Executor executor;
    private static final int REQUEST_CODE_LOGIN = 2021;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.private_audio_activity);

        Objects.requireNonNull(getSupportActionBar()).hide();

        executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(PrivateAudioActivity.this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);

                        Intent intent = new Intent(getBaseContext(), ActivityPassword.class);
                        startActivityForResult(intent, REQUEST_CODE_LOGIN);
                    }

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Toast.makeText(getApplicationContext(),
                                "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(getApplicationContext(), "Authentication failed",
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                });


        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for Private Audios")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use your user password")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOGIN && resultCode != RESULT_OK) {
            finish();
        }
    }
}
