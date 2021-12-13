package player.sazzer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import player.sazzer.DataTypes.Song;

public class AudioServiceBinder extends Service implements MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {
    // Guarda la ubicacion del archivo
    public static String mBroadcasterServiceBinder = "player.sazzer.action.UPDATE_AUDIOBINDER";

    // El reproductor mismo para reproducir contenido.
    private MediaPlayer audioPlayer;

    // Contenedor para las canciones siguientes.
    private ArrayList<Song> songs = new ArrayList<>();

    private NowPlayingManager manager;
    private SongFinder songFinder;

    private AudioManager am = null;

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

        manager.updateMediaSessionPosition( getCurrentAudioPosition() );

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

        handler.postDelayed(this::updateContent,mil);
    }

    private Intent playIntent;

    private final ServiceConnection notificationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("notificationConnection","Service has started");
            manager = ((NowPlayingManager.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    public void onCreate()
    {
        Log.d("AudioServiceBinder","onCreate: Preparing");
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(mBroadcasterServiceBinder);
        this.registerReceiver(mReceiver,filter);

        audioPlayer = new MediaPlayer();
        audioPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        songFinder = new SongFinder( getApplicationContext() , MediaStore.Audio.Media.EXTERNAL_CONTENT_URI );

        am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        initAudioPlayer();

        if( playIntent == null ) {
            playIntent = new Intent(this, NowPlayingManager.class);
            bindService(playIntent, notificationConnection, Context.BIND_AUTO_CREATE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(playIntent);
            } else {
                startService(playIntent);
            }
        }

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
        stopService(playIntent);
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onAudioFocusChange(int focusChange) {
        if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        {
            audioPlayer.stop();
            manager.setPauseIcon( false, getCurrentAudioPosition() );
        }
        else if(focusChange == AudioManager.AUDIOFOCUS_GAIN)
        {
            audioPlayer.start();
            manager.setPauseIcon( true, getCurrentAudioPosition() );
        }
        else if(focusChange == AudioManager.AUDIOFOCUS_LOSS)
        {
            audioPlayer.stop();
            manager.setPauseIcon( false, getCurrentAudioPosition() );
            // Stop or pause depending on your need
        }
    }

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
        audioPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        audioPlayer.setOnPreparedListener(this);
        audioPlayer.setOnCompletionListener(this);
        audioPlayer.setOnErrorListener(this);
    }

    public int getCurrentAudioPosition()
    {
        int ret = 0;
        if(audioPlayer != null)
        {
            ret = audioPlayer.getCurrentPosition();
        }
        return ret;
    }

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

    boolean readingByteFile = false;
    File tempMp3;
    public void LoadFromByteArray( byte[] soundByteArray )
    {
        try{
            tempMp3 = File.createTempFile("sazzer", "mp3", getCacheDir());
            // Tell the File to delete itself.
            // TODO: This deletes the file when the JVM is closed, not the app.
            // There has to be a way to determine the location of the file to a temporary location.
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            // Write to the temporary file to then directly play.
            fos.write(soundByteArray);
            fos.close();

            // Restart music player.
            // This is needed because we can run out of resources if this is not done.
            audioPlayer.reset();
            FileInputStream fis = new FileInputStream(tempMp3);
            audioPlayer.setDataSource(fis.getFD());

            readingByteFile = true;
            audioPlayer.prepare();
            audioPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSong(int songIndex) {
        songPosn = songIndex;
    }

    public void playSong() throws IOException {
        audioPlayer.reset();
        //Log.d("AudioPlayerCheck",String.format("%s",audioPlayer));
        Song playSong = songs.get(songPosn);
        Uri trackUri = Uri.fromFile( new File(playSong.getSongPath()) );

        if( TextUtils.isEmpty(trackUri.toString()) )
            return;

        //Log.d("AudioServiceBinder:playSong()","Looking for song in " + trackUri.toString());
        try {
            audioPlayer.setDataSource(getApplicationContext(), trackUri);
            audioPlayer.prepareAsync();
            // Create the manager to send the notification.

            manager.updateSong( songs.get(songPosn),  true );
            manager.setPauseIcon( audioPlayer.isPlaying(), 0 );

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(NowPlayingManager.mBroadcasterNotificationAction);
            broadcastIntent.putExtra("currentSong", MusicHelpers.ConvertSongsToJSONTable(playSong));
            getApplicationContext().sendBroadcast(broadcastIntent);

            refresh(900);
        } catch (Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
    }

    private void changeSong( AudioServiceAction Action )
    {
        int offset = Action == AudioServiceAction.AUDIO_SERVICE_ACTION_NEXT_SONG ? 1 : -1;
        // Is there a song available to play next?
        int oldsum = songPosn;
        int newsum = (songPosn += offset);
        Log.d("dsdfa", String.format("%d - %d", newsum, songs.size()-1));
        if( newsum < 0 )
        {
            songPosn = 0;
            if( getAudioProgress() > 1 ) {
                setProgress(0);
            }
            return;
        }
        if( newsum > (songs.size()-1) ) {
            songPosn = songs.size()-1;
            return;
        }

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle Extras = intent.getExtras();
        if( Extras == null )
            return START_STICKY;
        if( Extras.containsKey("AUDIO_ACTION") )
            return START_STICKY;

        //if( intent.getSerializableExtra("AUDIO_ACTION") != null )
        {
            AudioServiceAction Action = (AudioServiceAction) intent.getSerializableExtra("AUDIO_ACTION");

            switch (Action)
            {
                case AUDIO_SERVICE_ACTION_NEXT_SONG:
                {
                    changeSong(AudioServiceAction.AUDIO_SERVICE_ACTION_NEXT_SONG);
                    break;
                }
                case AUDIO_SERVICE_ACTION_PREV_SONG:
                {
                    changeSong(AudioServiceAction.AUDIO_SERVICE_ACTION_PREV_SONG);
                    break;
                }
                case AUDIO_SERVICE_ACTION_TOGGLE_PLAY:
                {
                    if( audioPlayer.isPlaying() )
                        audioPlayer.pause();
                    else
                        audioPlayer.start();

                    {
                        Intent broadcastIntent = new Intent();
                        broadcastIntent.setAction(DetailsActivity.mBroadcasterAudioAction);
                        broadcastIntent.putExtra("needsPause", !audioPlayer.isPlaying());
                        getApplicationContext().sendBroadcast(broadcastIntent);
                        manager.setPauseIcon( audioPlayer.isPlaying(), getCurrentAudioPosition() );
                    }

                    break;
                }
                default:
                    break;
            }
        }
        return START_STICKY;
    }

    // This will be used to receive actions from other intents and services that need to interact
    // with.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d("AudioPlayerCheck",String.format("%s",audioPlayer));
            //String action = intent.getAction();
            //Log.d("AudioServiceBinder","reciever's onReceive was called.");

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
                        manager.updateMediaSessionPosition( getCurrentAudioPosition() );
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
                        Intent broadcastIntent = new Intent();
                        broadcastIntent.setAction(DetailsActivity.mBroadcasterAudioAction);
                        broadcastIntent.putExtra("needsPause", !audioPlayer.isPlaying());
                        getApplicationContext().sendBroadcast(broadcastIntent);
                    }
                    break;
                }

                case AUDIO_SERVICE_ACTION_NEXT_SONG:
                case AUDIO_SERVICE_ACTION_PREV_SONG:
                {
                    changeSong(Action);
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
                case AUDIO_SERVICE_ACTION_FETCH_ALBUMS:
                {
                    Log.d("AUDIO_SERVICE_ACTION_FETCH_SONGS","Fetching songs");
                    songFinder.GenerateSongList();
                    songFinder.getAlbums();
                    //songs = songFinder.getList();
                    break;
                }
                case AUDIO_SERVICE_ACTION_OBTAIN_SONGS_TO_DISPLAY:
                {
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(MainActivity.mBroadcasterMainActivity);
                    broadcastIntent.putExtra("Audio.SongArray", MusicHelpers.ConvertSongsToJSONTable(songs) );
                    getApplicationContext().sendBroadcast(broadcastIntent);
                    break;
                }

                case AUDIO_SERVICE_ACTION_OBTAIN_ALBUMS_TO_DISPLAY:
                {
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(AllAlbumView.mBroadcasterMainActivity);
                    broadcastIntent.putExtra("Audio.SongArray", MusicHelpers.ConvertSongsToJSONTable(songs) );
                    getApplicationContext().sendBroadcast(broadcastIntent);
                    break;
                }

                case AUDIO_SERVICE_ACTION_UPDATE_DETAILED_INFO:
                {
                    Log.d("AudioService","Sending back info");
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(DetailsActivity.mBroadcasterAudioAction);
                    Song track = songs.get(songPosn);
                    broadcastIntent.putExtra("songName", track.getTitle());
                    broadcastIntent.putExtra("songArtist", track.getArtist());
                    broadcastIntent.putExtra("songArt", track.getAlbum().getAlbumArt());
                    getApplicationContext().sendBroadcast(broadcastIntent);
                }

                case AUDIO_SERVICE_ACTION_LOAD_AUDIO_BYTE_ARRAY:
                {
                    Log.d("AudioService","Load Audio Byte Array");
                    // Decode the intent action recieved as it must be converted back into a
                    // generated byte array.
                    String data = intent.getStringExtra("Audio.ByteArray");
                    byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
                    LoadFromByteArray(bytes);
                }
            }
        }
    };

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d("MediaPlayer","Playing a track");
        Log.d("MediaPlayer","Song seems to be " + audioPlayer.getDuration());

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(DetailsActivity.mBroadcasterAudioAction);
        Song track = songs.get(songPosn);
        broadcastIntent.putExtra("songName", track.getTitle());
        broadcastIntent.putExtra("songArtist", track.getArtist());
        broadcastIntent.putExtra("songArt", track.getAlbum().getAlbumArt());
        getApplicationContext().sendBroadcast(broadcastIntent);

        // Update the playlist if it happens to be available and shown on the screen.
        broadcastIntent = new Intent();
        broadcastIntent.setAction(PlaylistView.mBroadcasterPlayListView);
        broadcastIntent.putExtra("position", songPosn );
        getApplicationContext().sendBroadcast(broadcastIntent);

        am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        mp.start();
        manager.setPauseIcon( audioPlayer.isPlaying(), 0 );
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.reset();
        Log.d("MediaPlayer","Completed track");
        // Was the audio file that was just played a recording?
        if( readingByteFile )
        {
            tempMp3.delete();
            readingByteFile = false;
            return;
        }
        // Is there a song available to play next?
        if( (songPosn+1) > songs.size()-1 ) {
            // No, we're done, stop everything.
            //if (manager != null)
                //manager.cancelNotification();
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