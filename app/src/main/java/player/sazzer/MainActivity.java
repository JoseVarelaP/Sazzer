package player.sazzer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
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

public class MainActivity extends AppCompatActivity implements MediaPlayerControl {
    public static final int REQUEST_CODE = 1001;
    public static final int REQUEST_CODE_EXTERNAL_STORAGE = 1002;

    Intent playIntent = null;

    ArrayList<Song> songList;

    private AudioServiceBinder musicSrv;

    private ServiceConnection musicConnection = new ServiceConnection() {

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
        }

        ListView songView = findViewById(R.id.songList);
        songList = new ArrayList<>();

        getSongList();

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

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        // Vamos a buscar música que está en la memoria externa.
        // TODO (probablemente: Preguntar luego por ubicaciones especiales, para que la gente pueda
        // colocar su propia música.
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        // Hora de buscar
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist, thisAlbum));
            }
            while (musicCursor.moveToNext());
        }
    }

    public void songPicked(View view) throws IOException {
        Log.d("MainActivity","Set Song: " + (view.getTag().toString()));
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        /*
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
        */
    }

    /*
    void loadAudios () {
        String [] columns = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
        };

        String order = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

        Cursor cursor =  getBaseContext().getContentResolver().query (MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, null, null, order);
        if (cursor == null) return;

        LinkedList<Song> artists = new LinkedList<> ();

        for (int i = 0; i < cursor.getCount (); i++) {
            cursor.moveToPosition (i);
            long sId;
            String sTitle,sArtist;

            int index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            //long id = cursor.getLong(index);
            sId = cursor.getLong(index);

            index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            sArtist = cursor.getString(index);

            index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            sTitle = cursor.getString(index);

            //index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            //audioModel.songPath = cursor.getString(index);

            Song cSong = new Song (sId, sTitle, sArtist);
            artists.add (cSong);
        }

        cursor.close ();
    }
    */

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
                    //loadAudios ();
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

    @Override
    public void start() {

    }

    @Override
    public void pause() {

    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }

    @Override
    public void seekTo(int pos) {

    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
