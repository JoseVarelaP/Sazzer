package player.sazzer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import player.sazzer.DataTypes.TimeSpace;

public class DetailsActivity extends Activity {
    SeekBar sbProgress;
    ImageButton button,prev,next;
    TextView curTime,totalTime,Nombre,Artista, lyric;
    ScrollView lyricContainer;
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
            Log.d("Record", "Starting record area");
            if (lyricContainer.getVisibility() == View.INVISIBLE)
            {
                lyricContainer.setVisibility(View.VISIBLE);
                if (!lyric.getText().toString().equals(""))
                    return;
                lyric.setText(getString(R.string.downloadingLyrics));
                LyricSong lyricSong = new LyricSong(Nombre.getText().toString(), Artista.getText().toString());
                new DownloadLyrics(this, lyricSong).start();
            }
            else lyricContainer.setVisibility(View.INVISIBLE);
        });

        ImageView record = findViewById(R.id.recordSongButton);
        record.setColorFilter( R.color.recordButtonColor );
        record.setOnClickListener(v -> {
            finish();
        });
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
    protected void onDestroy () { super.onDestroy(); }

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
    protected void onStart() { super.onStart (); }

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
