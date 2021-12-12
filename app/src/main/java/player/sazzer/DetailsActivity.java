package player.sazzer;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

import player.sazzer.DataBase.AppDataBase;
import player.sazzer.DataBase.Song;
import player.sazzer.DataTypes.TimeSpace;

public class DetailsActivity extends AppCompatActivity implements SensorEventListener {
    SeekBar sbProgress;
    ImageButton button,prev,next;
    TextView curTime,totalTime,Nombre,Artista, lyric;
    ScrollView lyricContainer;
    ImageView albumArt;

    SensorManager sensorManager;
    Sensor accelerometer;
    private long mShakeTime = 0;

    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;

    MediaRecorder mediaRecorder;
    File fileAudio;

    AppDataBase database;


    private final BroadcastReceiver musicDataReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();

            if( extras == null )
                return;

            if( extras.getBoolean("needsPause",false) )
            {
                button.setImageResource(R.drawable.ic_play_white_48dp);
                return;
            } else {
                button.setImageResource(R.drawable.ic_pause_white_48dp);
            }

            String nCancion = intent.getStringExtra("songName");
            String nArtista = intent.getStringExtra("songArtist");
            String nArt = intent.getStringExtra("songArt");

            if( nCancion != null && (!Nombre.getText().equals(nCancion)) )
                Nombre.setText( nCancion );
            if( nArtista != null )
                Artista.setText( nArtista );
            if( nArt != null )
                albumArt.setImageBitmap( MusicHelpers.getAlbumImage(nArt) );

            int songProgress = extras.getInt("Progress");
            int songMax = extras.getInt("TotalTime");

            TimeSpace timeCur = new TimeSpace(songProgress);
            TimeSpace timeMax = new TimeSpace(songMax);

            curTime.setText( timeCur.convertToReadableMusicTime() );

            totalTime.setText( timeMax.convertToReadableMusicTime() );
            sbProgress.setMax(songMax);

            sbProgress.setProgress( songProgress );
        }
    };

    private IntentFilter mIntentFilter;

    public static final String mBroadcasterAudioAction = "player.sazzer.action.UPDATE_PROGRESS";

    @Override
    protected void onNewIntent(Intent intent) {

        //Log.d("onNewIntent","Loading from intent");
        String nCancion = intent.getStringExtra("songName");
        String nArtista = intent.getStringExtra("songArtist");
        String nArt = intent.getStringExtra("songArt");

        Nombre.setText( nCancion );
        Artista.setText( nArtista );
        albumArt.setImageBitmap( MusicHelpers.getAlbumImage(nArt) );

        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate (@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("onCreate", "Creating new screen");
        setContentView(R.layout.activity_details);
        Objects.requireNonNull(getSupportActionBar()).hide();
        curTime = findViewById(R.id.curTime);
        totalTime = findViewById(R.id.totalTime);
        Nombre = findViewById( R.id.songName );
        Artista = findViewById( R.id.artistName );
        albumArt = findViewById( R.id.imageCover );
        lyric = findViewById(R.id.lyricText);
        lyricContainer = findViewById(R.id.lyricContainer);

        Nombre.setSelected(true);
        Artista.setSelected(true);

        onNewIntent(this.getIntent());

        database = Room.databaseBuilder (this, AppDataBase.class, "mySongs").allowMainThreadQueries().build ();

        sbProgress = findViewById(R.id.sbProgress);
        sbProgress.setOnSeekBarChangeListener(new MySeekBarChangeListener());

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcasterAudioAction);

        button = findViewById(R.id.TogglePlay);
        button.setColorFilter( R.color.nowPlayingbuttonColor );
        button.setOnClickListener(v -> sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_TOGGLE_PLAY) ));

        prev = findViewById(R.id.PrevSong);
        prev.setColorFilter( R.color.nowPlayingbuttonColor );
        prev.setOnClickListener(v -> {
            lyric.setText("");
            lyricContainer.setVisibility(View.INVISIBLE);
            sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_PREV_SONG) );
        });

        next = findViewById(R.id.NextSong);
        next.setColorFilter( R.color.nowPlayingbuttonColor );
        next.setOnClickListener(v -> {
            lyric.setText("");
            lyricContainer.setVisibility(View.INVISIBLE);
            sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_NEXT_SONG) );
        });

        ImageView playList = findViewById(R.id.playListButton);
        playList.setColorFilter( R.color.nowPlayingbuttonColor );
        playList.setOnClickListener(v -> {
            Log.d("PlayList", "Starting playlist area");
            sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_EXPORT_QUEUE_TO_PLAYLIST) );
        });

        ImageView lyrics = findViewById(R.id.lyricsButton);
        lyrics.setColorFilter( R.color.nowPlayingbuttonColor );
        lyrics.setOnClickListener(v -> {
            if (lyricContainer.getVisibility() == View.INVISIBLE)
            {
                lyricContainer.setVisibility(View.VISIBLE);
                if (!lyric.getText().toString().equals(""))
                    return;

                String temp_ly = searchLyricInDB();

                if (temp_ly != null) {
                    lyric.setText(temp_ly);
                    Log.i("DB", "Lyrics DB");
                    return;
                }

                lyric.setText(getString(R.string.downloadingLyrics));
                LyricSong lyricSong = new LyricSong(Nombre.getText().toString().toLowerCase(),
                        Artista.getText().toString().toLowerCase());
                new DownloadLyrics(this, lyricSong).start();
            }
            else lyricContainer.setVisibility(View.INVISIBLE);
        });

        ImageView record = findViewById(R.id.recordSongButton);
        record.setColorFilter( R.color.recordButtonColor );
        record.setOnClickListener(v -> recordAudio());


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private String searchLyricInDB () {
        Song song = database.songDao().findByTitleAndArtist(Nombre.getText().toString(), Artista.getText().toString());

        if (song == null)
            return null;
        return song.lyric;
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Nombre.setText( savedInstanceState.getString("SongName") );
        Artista.setText( savedInstanceState.getString("SongArtist") );
    }

    @Override
    protected void onSaveInstanceState (@NonNull Bundle outState) {
        super.onSaveInstanceState (outState);
        outState.putString("SongName", Nombre.getText().toString() );
        outState.putString("SongArtist", Artista.getText().toString() );
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        sensorManager.unregisterListener((SensorEventListener) this);
    }

    @Override
    protected void onResume() {
        super.onResume ();
        sendBroadcast(MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_UPDATE_DETAILED_INFO));
        this.registerReceiver(musicDataReciever, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(musicDataReciever);
    }

    @Override
    protected void onStart() {
        super.onStart ();
        sensorManager.registerListener((SensorEventListener) this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                //Log.i("HERE","MOVE");
                detectShake(sensorEvent);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();

        if ((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;

            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            //float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            //float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            if (Math.abs(gX) > SHAKE_THRESHOLD) {
                sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_TOGGLE_PLAY) );
            }
        }
    }

    private void recordAudio() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO
            }, 2000);
            return;
        }

        if (mediaRecorder == null) {
            String dateTime = DateFormat.getDateTimeInstance().
                    format(new Date()).replace(' ', '_').replace(':','-');
            String format = String.format("/%s.mp3",dateTime);
            File folder = new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name));

            if (!folder.exists()) {
                boolean re = folder.mkdirs();
                Log.i("FOLDER:", String.valueOf(re));
            }
            fileAudio = new File(folder, format);
            if (!fileAudio.exists()) {
                try {
                    boolean  r = fileAudio.createNewFile();
                    Log.i("FILE:", String.valueOf(r));
                } catch (IOException exception) {
                    exception.printStackTrace();
                    return;
                }
            }
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(fileAudio.getPath());

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                Toast.makeText(this,"Recording Started...", Toast.LENGTH_LONG)
                        .show();
                Log.i("SUCCESS_AUDIO", fileAudio.getPath());
            } catch (IOException exception) {
                Log.e("ERROR_AUDIO", exception.toString());
            }
        } else {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            Toast.makeText(this,"Recording Finished...", Toast.LENGTH_LONG)
                    .show();
            Log.i("SUCCESS_AUDIO", fileAudio.getPath() + " --Finish");
        }
    }

    class MySeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        int curVal = 0;
        @Override
        public void onProgressChanged (SeekBar seekBar, int i, boolean b) {
            if (b) {
                curVal = i;
            }
        }

        @Override
        public void onStartTrackingTouch (SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch (SeekBar seekBar) {
            Intent forThePlayer = new Intent();
            forThePlayer.setAction(AudioServiceBinder.mBroadcasterServiceBinder);
            forThePlayer.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_UPDATE_PROGRESS);
            forThePlayer.putExtra("Audio.SeekProgress", curVal );
            sendBroadcast(forThePlayer);
        }

    }
}
