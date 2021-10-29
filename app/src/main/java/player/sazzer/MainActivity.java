package player.sazzer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
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
import android.widget.MediaController.MediaPlayerControl;

import player.sazzer.AudioServiceBinder.MusicBinder;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE = 1001;
    public static final int REQUEST_CODE_EXTERNAL_STORAGE = 1002;

    Intent playIntent = null;

    ArrayList<Song> songList;

    private AudioServiceBinder musicSrv;

    NotificationManager notificationManager;

    private final ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            musicSrv = binder.getService();
            musicSrv.setList(songList);
            //musicBound = true;
            Log.d("musicConnection","Service Connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //musicBound = false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        musicSrv = new AudioServiceBinder();

        // Hay que pedir el elemento para cargar los audios.
        // Si no, entonces tendremos un error/choque debido a la estancia de acceso ilegal de archivos.
        int perm = getBaseContext ().checkSelfPermission (Manifest.permission.READ_EXTERNAL_STORAGE);
        if (perm != PackageManager.PERMISSION_GRANTED) {
            requestPermissions (
                    new String [] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    REQUEST_CODE_EXTERNAL_STORAGE
            );
        } else {
            GenerateMainSongList();
        }
    }

    protected void GenerateMainSongList()
    {
        ListView songView = findViewById(R.id.songList);
        songList = new ArrayList<>();

        //getSongList(MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
        getSongList(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);

        ListadoMusica listMusica = new ListadoMusica(this, songList);
        songView.setAdapter(listMusica);

        musicSrv.setList(songList);
    }

    @Override
    protected void onStart() {
        Log.d("onStart","Starting");
        super.onStart();
        musicSrv.setContext(getApplicationContext());
        musicSrv.initAudioPlayer();
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

    public void songPicked(View view) throws IOException {
        Log.d("MainActivity","Set Song: " + (view.getTag().toString()));
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0 && grantResults [0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText (getBaseContext(),"¡Permiso concedido!", Toast.LENGTH_LONG).show ();
                }
                break;
            case REQUEST_CODE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults [0] == PackageManager.PERMISSION_GRANTED) {
                    GenerateMainSongList ();
                }
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
