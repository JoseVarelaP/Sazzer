package player.sazzer.LocalLogActivities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.Executor;

import player.sazzer.R;

public class PrivateAudioActivity extends AppCompatActivity {
    Executor executor;
    private static final int REQUEST_CODE_LOGIN = 2021;

    protected void LoadAudioFiles()
    {
        String path = Environment.getExternalStorageDirectory().toString()+"/"+getString(R.string.app_name);
        File directory = new File(path);

        if( !directory.exists() ) {
            Log.d("Files", "Directory does not exist!");
            return;
        }

        if( !directory.isDirectory() )
            return;

        File[] files = directory.listFiles();
        Log.d("Files", "Size: "+ files.length);
        for (int i = 0; i < files.length; i++)
        {
            Log.d("Files", "FileName:" + files[i].getName());
        }

        // TODO: Show the audio files on a list.
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.private_audio_activity);

        LoadAudioFiles();

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
                        LoadAudioFiles();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(getApplicationContext(), "Authentication failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });


        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_ms1))
                .setSubtitle(getString(R.string.biometric_ms2))
                .setNegativeButtonText(getString(R.string.biometric_ms3))
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
