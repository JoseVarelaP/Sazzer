package player.sazzer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;

import player.sazzer.Adapters.PlaylistRecyclerViewAdapter;

public class MainActivity extends AppCompatActivity implements PlaylistRecyclerViewAdapter.ItemClickListener {
    public static final int REQUEST_CODE_EXTERNAL_STORAGE = 1002;

    Intent playIntent = null;
    ArrayList<Song> songList;
    private AudioServiceBinder musicSrv;
    NotificationManager notificationManager;

    private final ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {}

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_main);

        // Hay que pedir el elemento para cargar los audios.
        // Si no, entonces tendremos un error/choque debido a la estancia de acceso ilegal de archivos.
        int perm = getBaseContext ().checkSelfPermission (Manifest.permission.READ_EXTERNAL_STORAGE);
        if (perm != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    REQUEST_CODE_EXTERNAL_STORAGE);
        } else {
            GenerateMainSongList();
        }
    }

    protected void GenerateMainSongList()
    {
        RecyclerView songView = findViewById(R.id.songList);
        songView.setLayoutManager(new LinearLayoutManager(this));
        songList = new ArrayList<>();

        //getSongList(MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
        getSongList(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);

        PlaylistRecyclerViewAdapter listMusica = new PlaylistRecyclerViewAdapter(this, songList);
        listMusica.setClickListener(this);
        songView.setAdapter(listMusica);
    }

    @Override
    protected void onStart() {
        Log.d("onStart","Starting");
        super.onStart();
        if (playIntent == null) {
            Log.d("onStart","Intent is null, starting service.");
            playIntent = new Intent(this, AudioServiceBinder.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
            Log.d("onStart","Done with setup of services.");
        }
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv = null;
        super.onDestroy();
    }

    public void getSongList(Uri musicUri) {
        ContentResolver musicResolver = getContentResolver();
        // Vamos a buscar música que está en la memoria externa.
        // TODO (probablemente: Preguntar luego por ubicaciones especiales, para que la gente pueda
        // colocar su propia música.

        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.ALBUM + " ASC, " + MediaStore.Audio.Media.TRACK + " ASC";

        // Check if we can create the notification channel to show it.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O )
        {
            NotificationChannel channel = new NotificationChannel(NowPlayingManager.CHANNEL_ID,
                    "Now Playing", NotificationManager.IMPORTANCE_LOW);

            notificationManager = getSystemService(NotificationManager.class);
            if( notificationManager != null ) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Cursor musicCursor = musicResolver.query(musicUri, null, selection, null, sortOrder);

        // Hora de buscar
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            //int albumId = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int column_index = musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String pathId = musicCursor.getString(column_index);

                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist, thisAlbum, pathId));
            }
            while (musicCursor.moveToNext());
        }

        // Needs to be closed to avoid memory leak.
        if(musicCursor != null)
            musicCursor.close();
    }

    @Override
    public void onItemClick(View view, int position) {
        Log.d("MainActivity","Set Song: " + position );

        // Broadcast a music list reset to the service.
        sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_CLEAN_QUEUE) );

        sendBroadcast( MusicHelpers.createIntentToUpdateMusicArray(songList) );

        Intent intent = new Intent();
        intent.setAction(AudioServiceBinder.mBroadcasterServiceBinder);
        intent.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_UPDATE_SONG_ID);
        intent.putExtra("Audio.SongID", position);
        sendBroadcast(intent);

        sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_PLAY_SONG) );

        Intent nt = MusicHelpers.sendToDetailedSongInfo(MainActivity.this, songList.get(position), musicSrv);
        nt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(nt);
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults [0] == PackageManager.PERMISSION_GRANTED) {
                    GenerateMainSongList ();
                }
                break;
            default:
                break;
        }
    }

    /**
     * Callback invocado después de llamar a startActivityForResult
     *
     * @param requestCode código de verificación de la llamadas al método
     * @param resultCode resultado: OK, CANCEL, etc.
     * @param data información resultante, si existe
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
