package player.sazzer.LocalLogActivities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executor;

import player.sazzer.Adapters.PlaylistRecyclerViewAdapter;
import player.sazzer.Adapters.PrivateAudioViewAdapter;
import player.sazzer.AudioServiceAction;
import player.sazzer.DataTypes.Song;
import player.sazzer.EncryptorManager;
import player.sazzer.MusicHelpers;
import player.sazzer.R;

public class PrivateAudioActivity extends AppCompatActivity implements PrivateAudioViewAdapter.ItemClickListener {
    Executor executor;
    private static final int REQUEST_CODE_LOGIN = 2021;
    ArrayList<Song> audioFiles;
    EncryptorManager EM;

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
        //audioFiles = new ArrayList<>();
        Log.d("Files", "Size: "+ files.length);
        for (int i = 0; i < files.length; i++)
        {
            Song temp = new Song(i, files[i].getName(), files[i].getName(), "test", 0, 0);
            audioFiles.add(temp);
            Log.d("Files", "FileName:" + files[i].getName());
        }

        // TODO: Show the audio files on a list.
        RecyclerView songView = findViewById(R.id.audioFiles);
        songView.setLayoutManager(new LinearLayoutManager(this));

        PrivateAudioViewAdapter listMusica = new PrivateAudioViewAdapter(this, audioFiles, null);
        listMusica.setClickListener( this );
        songView.setAdapter(listMusica);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioFiles = new ArrayList<>();
        EM = new EncryptorManager( getApplicationContext() );
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

    @Override
    public void onItemClick(View view, int position) {
        Log.d("wooooo","yeah");

        // When the file has been selected, begin the decrypt process.
        Song audio = audioFiles.get(position);

        String audioPath = EM.ReadEncryptedFile( audio.getSongPath() );
        if( audioPath == null )
            return;

        Intent tm = MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_LOAD_AUDIO_BYTE_ARRAY);
        tm.putExtra("Audio.ByteArray", audioPath);

        sendBroadcast(tm);
    }
}
