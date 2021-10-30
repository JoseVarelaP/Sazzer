package player.sazzer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.sql.Date;
import java.util.concurrent.TimeUnit;

public class DetailsActivity extends Activity {
    //private Handler audioProgressUpdateHandler = null;
    MediaPlayer player;
    SeekBar sbProgress;
    ImageButton button,prev,next;
    TextView curTime,totalTime,Nombre,Artista;
    ImageView albumArt;


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

            if( nCancion != null )
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
        curTime = findViewById(R.id.curTime);
        totalTime = findViewById(R.id.totalTime);
        Nombre = findViewById( R.id.songName );
        Artista = findViewById( R.id.artistName );
        albumArt = findViewById( R.id.imageCover );

        onNewIntent(this.getIntent());

        sbProgress = findViewById(R.id.sbProgress);
        sbProgress.setOnSeekBarChangeListener(new MySeekBarChangeListener());

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcasterAudioAction);

        button = findViewById(R.id.TogglePlay);
        button.setOnClickListener(v -> {
            Intent forThePlayer = new Intent();
            forThePlayer.setAction(AudioServiceBinder.mBroadcasterServiceBinder);
            forThePlayer.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_TOGGLE_PLAY);
            forThePlayer.putExtra("Audio.TogglePlay",true);
            sendBroadcast(forThePlayer);
        });

        prev = findViewById(R.id.PrevSong);
        prev.setOnClickListener(v -> {
            Intent forThePlayer = new Intent();
            forThePlayer.setAction(AudioServiceBinder.mBroadcasterServiceBinder);
            forThePlayer.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_PREV_SONG);
            sendBroadcast(forThePlayer);
        });

        next = findViewById(R.id.NextSong);
        next.setOnClickListener(v -> {
            Intent forThePlayer = new Intent();
            forThePlayer.setAction(AudioServiceBinder.mBroadcasterServiceBinder);
            forThePlayer.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_NEXT_SONG);
            sendBroadcast(forThePlayer);
        });

    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState (@NonNull Bundle outState) {
        super.onSaveInstanceState (outState);

        //outState.putString ("SONG", mediaUri != null ? mediaUri.toString (): "");
        outState.putInt ("PROGRESS", player != null ?  player.getCurrentPosition () : -1);
        outState.putBoolean ("ISPLAYING", player != null && player.isPlaying ());
    }

    @Override
    protected void onDestroy () { super.onDestroy(); }

    @Override
    protected void onResume() {
        super.onResume ();
        //IntentFilter intentFilter = new IntentFilter(mBroadcasterAudioAction);
        this.registerReceiver(musicDataReciever, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(musicDataReciever);
    }

    @Override
    protected void onStart() { super.onStart (); }

    class MySeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        int curVal = 0;
        @Override
        public void onProgressChanged (SeekBar seekBar, int i, boolean b) {
            if (b) {
                curVal = i;
                //audioServiceBinder.pauseAudio();
                //audioServiceBinder.setProgress( i );
                //audioServiceBinder.startAudio();
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
