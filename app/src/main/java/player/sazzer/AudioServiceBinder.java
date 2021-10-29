package player.sazzer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AudioServiceBinder extends Service implements MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,MediaPlayer.OnCompletionListener {

    // Guarda la ubicacion del archivo
    // private Uri audioFileUri = null;
    public static String mBroadcasterServiceBinder = "player.sazzer.action.UPDATE_AUDIOBINDER";

    // El reproductor mismo para reproducir contenido.
    private MediaPlayer audioPlayer;

    // Contexto, necesario para interactuar externalmente.
    private Context context = null;

    // Contenedor para las canciones siguientes.
    private ArrayList<Song> songs = new ArrayList<>();

    private NowPlayingManager manager;

    enum PlayerState {
        PLAYER_READY,
        PLAYER_LOADING,
        PLAYER_ERROR,
        PLAYER_NULL
    }

    PlayerState curPlayerState = PlayerState.PLAYER_NULL;

    private int songPosn = 0;

    //public Context getContext() { return context; }
    public void setContext(Context context) { this.context = context; }
    //public Uri getAudioFileUri() { return audioFileUri; }
    //public void setAudioFileUri(Uri audioFileUri) { this.audioFileUri = audioFileUri; }
    public void setProgress( int ms ) { this.audioPlayer.seekTo(ms); }
    //public MediaPlayer GetPlayer() { return audioPlayer; }

    public boolean isPlaying()
    {
        return audioPlayer.isPlaying();
    }

    void updateContent()
    {
        if( curPlayerState == PlayerState.PLAYER_ERROR || curPlayerState == PlayerState.PLAYER_NULL )
            return;

        if( !isPlaying() )
            return;

        //Log.d("runBinderUpdater","Song now is at " + getAudioProgress() + "%");
        manager.updateSong( songs.get(songPosn), getAudioProgress(), this );

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(DetailsActivity.mBroadcasterAudioAction);
        broadcastIntent.putExtra("Progress", getCurrentAudioPosition());
        broadcastIntent.putExtra("TotalTime", getTotalAudioDuration());
        getApplicationContext().sendBroadcast(broadcastIntent);

        refresh(1000);
    }

    void refresh(int mil)
    {
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateContent();
            }
        };

        handler.postDelayed(runnable,mil);
    }

    public void onCreate()
    {
        Log.d("AudioServiceBinder","onCreate: Preparing");
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(mBroadcasterServiceBinder);
        this.registerReceiver(mReceiver,filter);

        audioPlayer = new MediaPlayer();

        Log.d("AudioPlayerCheck",String.format("%s",audioPlayer));
        initAudioPlayer();

        Log.d("AudioServiceBinder","onCreate: Done");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if( audioPlayer != null ) {
            audioPlayer.release();
            audioPlayer = null;
        }
        unregisterReceiver(mReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void initAudioPlayer()
    {
        //audioPlayer.setWakeMode(getContext(), PowerManager.PARTIAL_WAKE_LOCK);
        audioPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        audioPlayer.setOnPreparedListener(this);
        audioPlayer.setOnCompletionListener(this);
        audioPlayer.setOnErrorListener(this);
    }

    // Regresa el progreso.
    public int getCurrentAudioPosition()
    {
        int ret = 0;
        if(audioPlayer != null)
        {
            ret = audioPlayer.getCurrentPosition();
        }
        return ret;
    }

    // Duracion completa de la canción.
    public int getTotalAudioDuration()
    {
        int ret = 0;
        if(audioPlayer != null)
        {
            ret = audioPlayer.getDuration();
        }
        return ret;
    }

    // Progreso actual de la canción.
    public int getAudioProgress()
    {
        int ret = 0;
        int currAudioPosition = getCurrentAudioPosition();
        int totalAudioDuration = getTotalAudioDuration();
        if(totalAudioDuration > 0) {
            ret = (currAudioPosition * 100) / totalAudioDuration;
        }
        return ret;
    }

    public class MusicBinder extends Binder {
        AudioServiceBinder getService() {
            return AudioServiceBinder.this;
        }
    }

    public void setSong(int songIndex) {
        songPosn = songIndex;
    }

    public Song getSong( int songIndex ) {
        if( songIndex > this.songs.size() )
            return null;

        return this.songs.get(songIndex);
    }

    public void setList( ArrayList<Song> canciones )
    {
        this.songs = canciones;
    }

    public Context getContext()
    {
        return this.context;
    }

    public void playSong() throws IOException {
        audioPlayer.reset();
        Log.d("AudioPlayerCheck",String.format("%s",audioPlayer));
        Song playSong = songs.get(songPosn);
        //songTitle = playSong.getTitle();
        //long currSong = playSong.getId();
        Uri trackUri = Uri.fromFile( new File(playSong.getAlbumArt()) );

        if( TextUtils.isEmpty(trackUri.toString()) )
            return;

        if( manager == null )
            manager = new NowPlayingManager( getApplicationContext() );

        Log.d("AudioServiceBinder:playSong()","Looking for song in " + trackUri.toString());
        try {
            audioPlayer.setDataSource(getApplicationContext(), trackUri);
            audioPlayer.prepare();
            audioPlayer.start();
            curPlayerState = PlayerState.PLAYER_LOADING;

            // Create the manager to send the notification.
            manager.updateSong( songs.get(songPosn), 0, this );
            refresh(900);
        } catch (Exception e) {
            curPlayerState = PlayerState.PLAYER_ERROR;
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        audioPlayer = new MediaPlayer();
        return START_STICKY;
    }

    // This will be used to recieve actions from other intents and services that need to interact
    // with.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("AudioPlayerCheck",String.format("%s",audioPlayer));
            //String action = intent.getAction();
            Log.d("AudioServiceBinder","reciever's onReceive was called.");

            AudioServiceAction Action = (AudioServiceAction) intent.getSerializableExtra("AUDIO_ACTION");

            switch (Action) {
                case AUDIO_SERVICE_ACTION_UPDATE_BINDER:
                {
                    String newsongsStr = intent.getStringExtra("Audio.SongArray");

                    if( newsongsStr != null && !newsongsStr.isEmpty() ) {
                        Gson gson = new Gson();

                        Type type = new TypeToken<List<Song>>(){}.getType();
                        List<Song> newsongs = gson.fromJson(newsongsStr, type);

                        if( newsongs.isEmpty() )
                        {
                            Log.e("BroacastReciever", "The given array is empty.");
                            return;
                        } else {
                            Log.w("BroacastReciever", "Obtained the array.");
                            songs = (ArrayList<Song>) newsongs;
                        }
                    }

                    if( intent.getIntExtra("Audio.SongID",-1) != -1 )
                    {
                        if( songs.size() > 0 )
                            setSong(intent.getIntExtra("Audio.SongID",-1));
                        else {
                            Log.e("BroacastReciever", "The song array is empty.");
                            return;
                        }
                    }

                    if( intent.getBooleanExtra("Audio.PlaySong",false) )
                    {
                        Log.d("Broadcast","Playing new song");
                        try {
                            playSong();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    break;
                }

                case AUDIO_SERVICE_ACTION_UPDATE_PROGRESS:
                {
                    int Progress = intent.getIntExtra("Audio.SeekProgress",-1);
                    if( intent.getBooleanExtra("Audio.TogglePlay",false) ){
                        Log.d("Audio.TogglePlay","Requested. Music from player " + audioPlayer + " is on state " + audioPlayer.isPlaying());
                        //boolean needsPause = false;
                        if( audioPlayer.isPlaying() )
                            audioPlayer.pause();

                        Intent broadcastIntent = new Intent();
                        broadcastIntent.setAction(DetailsActivity.mBroadcasterAudioAction);
                        //broadcastIntent.putExtra("needsPause", needsPause);
                    }

                    if( Progress > -1 )
                    {
                        setProgress( Progress );
                    }
                    break;
                }
            }
        }
    };

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d("MediaPlayer","Playing a track");
        Log.d("MediaPlayer","Song seems to be " + audioPlayer.getDuration());

        curPlayerState = PlayerState.PLAYER_READY;

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(DetailsActivity.mBroadcasterAudioAction);
        Song track = songs.get(songPosn);
        broadcastIntent.putExtra("songName", track.getTitle());
        broadcastIntent.putExtra("songArtist", track.getArtist());
        broadcastIntent.putExtra("songArt", track.getAlbumArt());
        context.sendBroadcast(broadcastIntent);

        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.reset();
        Log.d("MediaPlayer","Completed track");
        // Is there a song available to play next?
        if( (songPosn+1) > songs.size() )
            if( manager != null )
                manager.cancelNotification();

        setSong(songPosn+1);

        try {
            playSong();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }
}