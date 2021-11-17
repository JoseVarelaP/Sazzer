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
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AudioServiceBinder extends Service implements MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,MediaPlayer.OnCompletionListener {

    // Guarda la ubicacion del archivo
    // private Uri audioFileUri = null;
    public static String mBroadcasterServiceBinder = "player.sazzer.action.UPDATE_AUDIOBINDER";

    // El reproductor mismo para reproducir contenido.
    private MediaPlayer audioPlayer;

    // Contenedor para las canciones siguientes.
    private ArrayList<Song> songs = new ArrayList<>();

    private NowPlayingManager manager;
    private SongFinder songFinder;

    enum PlayerState {
        PLAYER_READY,
        PLAYER_LOADING,
        PLAYER_ERROR,
        PLAYER_NULL
    }

    PlayerState curPlayerState = PlayerState.PLAYER_NULL;

    private int songPosn = 0;
    public void setProgress( int ms ) { this.audioPlayer.seekTo(ms); }

    public boolean isPlaying()
    {
        return audioPlayer.isPlaying();
    }

    void updateContent()
    {
        if( !isPlaying() )
            return;

        //Log.d("runBinderUpdater","Song now is at " + getAudioProgress() + "%");
        manager.updateSong( songs.get(songPosn), getAudioProgress(), this, false );

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
        final Runnable runnable = this::updateContent;

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
        songFinder = new SongFinder( getApplicationContext() , MediaStore.Audio.Media.EXTERNAL_CONTENT_URI );

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

    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public AudioServiceBinder getService() {
            return AudioServiceBinder.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
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
        return audioPlayer.getDuration();
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

    public void setSong(int songIndex) {
        songPosn = songIndex;
    }

    public void playSong() throws IOException {
        audioPlayer.reset();
        Log.d("AudioPlayerCheck",String.format("%s",audioPlayer));
        Song playSong = songs.get(songPosn);
        Uri trackUri = Uri.fromFile( new File(playSong.getAlbumArt()) );

        if( TextUtils.isEmpty(trackUri.toString()) )
            return;

        if( manager == null )
            manager = new NowPlayingManager( getApplicationContext() );

        Log.d("AudioServiceBinder:playSong()","Looking for song in " + trackUri.toString());
        try {
            audioPlayer.setDataSource(getApplicationContext(), trackUri);
            audioPlayer.prepareAsync();

            curPlayerState = PlayerState.PLAYER_LOADING;

            // Create the manager to send the notification.
            manager.updateSong( songs.get(songPosn), 0, this, true );
            refresh(900);
        } catch (Exception e) {
            curPlayerState = PlayerState.PLAYER_ERROR;
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
                    break;
                }

                case AUDIO_SERVICE_ACTION_PLAY_SONG:
                {
                    Log.d("Broadcast","Playing new song");
                    try {
                        playSong();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                case AUDIO_SERVICE_ACTION_UPDATE_SONG_ID:
                {
                    if( intent.getIntExtra("Audio.SongID",-1) != -1 )
                    {
                        if( songs.size() > 0 )
                            setSong(intent.getIntExtra("Audio.SongID",-1));
                        else {
                            Log.e("BroacastReciever", "The song array is empty.");
                            return;
                        }
                    }
                    break;
                }

                case AUDIO_SERVICE_ACTION_UPDATE_PROGRESS:
                {
                    int Progress = intent.getIntExtra("Audio.SeekProgress",-1);
                    Log.d("Audio.SeekProgress", String.valueOf(Progress));

                    if( Progress > -1 )
                    {
                        setProgress( Progress );
                    }
                    break;
                }

                case AUDIO_SERVICE_ACTION_TOGGLE_PLAY:
                {
                    if( audioPlayer.isPlaying() )
                        audioPlayer.pause();
                    else
                        audioPlayer.start();

                    {
                        Intent broadcastIntent = new Intent(); //= MusicHelpers.sendToDetailedSongInfo(getApplicationContext(), songs.get(songPosn), this);
                        broadcastIntent.setAction(DetailsActivity.mBroadcasterAudioAction);
                        broadcastIntent.putExtra("needsPause", !audioPlayer.isPlaying());
                        getApplicationContext().sendBroadcast(broadcastIntent);
                    }
                    break;
                }

                case AUDIO_SERVICE_ACTION_NEXT_SONG:
                case AUDIO_SERVICE_ACTION_PREV_SONG:
                {
                    int offset = Action == AudioServiceAction.AUDIO_SERVICE_ACTION_NEXT_SONG ? 1 : -1;
                    // Is there a song available to play next?
                    int oldsum = songPosn;
                    int newsum = (songPosn += offset);
                    if( newsum < 0 || newsum > songs.size() )
                        break;

                    songPosn = newsum;
                    setSong(songPosn);

                    if( Action == AudioServiceAction.AUDIO_SERVICE_ACTION_NEXT_SONG )
                    {
                        try {
                            playSong();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // When pressing the previous song button, usually it goes back to the start
                        // of the song, before actually going to the previous song.
                        if( getAudioProgress() > 1 )
                        {
                            setProgress(0);
                            songPosn = oldsum;
                        } else {
                            try {
                                playSong();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    break;
                }

                case AUDIO_SERVICE_ACTION_CLEAN_QUEUE:
                {
                    // If anything is currently playing, stop.
                    if( audioPlayer.isPlaying() ) {
                        audioPlayer.stop();
                        audioPlayer.reset();
                    }
                    songs.clear();
                    break;
                }

                case AUDIO_SERVICE_ACTION_EXPORT_QUEUE_TO_PLAYLIST:
                {
                    Log.d("AUDIO_SERVICE_ACTION_EXPORT_QUEUE_TO_PLAYLIST","Export to Playlist");
                    Intent set = MusicHelpers.sendToPlaylist(getApplicationContext(), songs);
                    set.putExtra("position", songPosn);
                    startActivity(set);
                    //getApplicationContext().sendBroadcast(set);
                    break;
                }
                case AUDIO_SERVICE_ACTION_FETCH_SONGS:
                {
                    Log.d("AUDIO_SERVICE_ACTION_FETCH_SONGS","Fetching songs");
                    songFinder.GenerateSongList();
                    songs = songFinder.getList();

                    //songs = songFinder.FindMusicByAlbumName("Canarias Virtua A");
                    //songs = songFinder.FindMusicByArtist("whyetc");
                    break;
                }
                case AUDIO_SERVICE_ACTION_OBTAIN_SONGS_TO_DISPLAY:
                {
                    Intent broadcastIntent = new Intent(); //= MusicHelpers.sendToDetailedSongInfo(getApplicationContext(), songs.get(songPosn), this);
                    broadcastIntent.setAction(MainActivity.mBroadcasterMainActivity);
                    broadcastIntent.putExtra("Audio.SongArray", MusicHelpers.ConvertSongsToJSONTable(songs) );
                    getApplicationContext().sendBroadcast(broadcastIntent);
                    break;
                }

                case AUDIO_SERVICE_ACTION_UPDATE_DETAILED_INFO:
                {
                    Log.d("AudioServioce","Sending back info");
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(DetailsActivity.mBroadcasterAudioAction);
                    Song track = songs.get(songPosn);
                    broadcastIntent.putExtra("songName", track.getTitle());
                    broadcastIntent.putExtra("songArtist", track.getArtist());
                    broadcastIntent.putExtra("songArt", track.getAlbumArt());
                    getApplicationContext().sendBroadcast(broadcastIntent);
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
        getApplicationContext().sendBroadcast(broadcastIntent);

        // Update the playlist if it happens to be available and shown on the screen.
        broadcastIntent = new Intent(); //= MusicHelpers.sendToDetailedSongInfo(getApplicationContext(), songs.get(songPosn), this);
        broadcastIntent.setAction(PlaylistView.mBroadcasterPlayListView);
        broadcastIntent.putExtra("position", songPosn );
        getApplicationContext().sendBroadcast(broadcastIntent);

        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.reset();
        Log.d("MediaPlayer","Completed track");
        // Is there a song available to play next?
        if( (songPosn+1) > songs.size()-1 ) {
            // No, we're done, stop everything.
            if (manager != null)
                manager.cancelNotification();
            return;
        }

        songPosn++;
        setSong(songPosn);

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