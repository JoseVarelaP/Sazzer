package player.sazzer;

import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;

public class AudioServiceBinder extends Service implements MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,MediaPlayer.OnCompletionListener {

    // Guarda la ubicacion del archivo
    private Uri audioFileUri = null;

    // El reproductor mismo para reproducir contenido.
    private MediaPlayer audioPlayer = new MediaPlayer();

    // Contexto, necesario para interactuar externalmente.
    private Context context = null;

    // Contenedor para las canciones siguientes.
    private ArrayList<Song> songs = new ArrayList<>();

    private String songTitle = "";

    int songPosn = 0;

    public final int UPDATE_AUDIO_PROGRESS_BAR = 1;

    private final IBinder musicBind = new MusicBinder();

    //public Context getContext() { return context; }
    public void setContext(Context context) { this.context = context; }
    //public Uri getAudioFileUri() { return audioFileUri; }
    public void setAudioFileUri(Uri audioFileUri) { this.audioFileUri = audioFileUri; }
    public void setProgress( int ms ) { this.audioPlayer.seekTo(ms); }
    //public MediaPlayer GetPlayer() { return audioPlayer; }

    /*
    public void startAudio()
    {
        initAudioPlayer();
        if(audioPlayer!=null) {
            audioPlayer.start();
        }
    }

    public void pauseAudio()
    {
        if(audioPlayer!=null) {
            audioPlayer.pause();
        }
    }

    public void stopAudio()
    {
        if(audioPlayer!=null) {
            audioPlayer.stop();
            destroyAudioPlayer();
        }
    }
    */

    public boolean isPlaying()
    {
        if(audioPlayer!=null){
            return audioPlayer.isPlaying();
        }
        return false;
    }

    public void onCreate()
    {
        super.onCreate();
        audioPlayer = new MediaPlayer();
        initAudioPlayer();
    }

    private void initAudioPlayer()
    {
        audioPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        audioPlayer.setOnPreparedListener(this);
        audioPlayer.setOnCompletionListener(this);
        audioPlayer.setOnErrorListener(this);
        /*
        try {
            if (audioPlayer == null) {
                audioPlayer = new MediaPlayer();

                if( TextUtils.isEmpty(getAudioFileUri().toString()) )
                    return;

                audioPlayer.setDataSource(getContext(), getAudioFileUri());

                audioPlayer.prepare();

            }
        }catch(IOException ex)
        {
            ex.printStackTrace();
        }
        */
    }

    // Destruye el reprodutor.
    /*
    public void destroyAudioPlayer()
    {
        if(audioPlayer!=null)
        {
            if(audioPlayer.isPlaying())
            {
                audioPlayer.stop();
            }
            audioPlayer.release();
            audioPlayer = null;
        }
    }
    */

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

    public void setList( ArrayList<Song> canciones )
    {
        this.songs = canciones;
    }

    public Context getContext()
    {
        return this.context;
    }

    public void SetContext( Context s )
    {
        this.context = s;
    }

    public void playSong() throws IOException {
        audioPlayer.reset();
        Song playSong = songs.get(songPosn);
        songTitle = playSong.getTitle();
        long currSong = playSong.getId();
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        if( TextUtils.isEmpty(trackUri.toString()) )
            return;

        Log.d("AudioServiceBinder:playSong()","Looking for song in " + trackUri.toString());
        try {
            audioPlayer.setDataSource(getContext(), trackUri);
            audioPlayer.setOnPreparedListener(this);
            audioPlayer.prepareAsync();
            Log.d("AudioServiceBinder:playSong()","Song seems to be " + audioPlayer.getDuration());
        } catch (Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
    }

    // Comienzan acciones de Override.
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d("MediaPlayer","Playing a track");
        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }
}