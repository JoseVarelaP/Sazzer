package player.sazzer.Settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

import player.sazzer.LocalLogActivities.ActivityPassword;
import player.sazzer.MainActivity;
import player.sazzer.R;

public class NewPasswordActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOGIN = 29;

    Executor executor;
    EditText newPassword;
    TextView message;
    Button button;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.password_private_activity);

        message = findViewById(R.id.mess_password);
        newPassword = findViewById(R.id.user_password2);
        button = findViewById(R.id.btn_next2);

        message.setText(getString(R.string.new_password));
        button.setText(getString(R.string.new_password_btn));

        button.setOnClickListener(v -> updatePassword());


        executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(NewPasswordActivity.this, executor,
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
                .setTitle(getString(R.string.biometric_ms1))
                .setSubtitle(getString(R.string.biometric_ms2))
                .setNegativeButtonText(getString(R.string.biometric_ms3))
                .build();

        biometricPrompt.authenticate(promptInfo);
    }


    private void updatePassword() {
        if (newPassword.getText().toString().equals("")) {
            Toast.makeText(this, "Input Something", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.KEY_SHARED_PREFERENCES,
                MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MainActivity.KEY_PASSWORD, newPassword.getText().toString());
        editor.apply();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOGIN && resultCode != RESULT_OK) {
            finish();
        }
    }
}
