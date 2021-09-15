package player.sazzer;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.MediaController.MediaPlayerControl;

import player.sazzer.AudioServiceBinder.MusicBinder;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MediaPlayerControl {
    public static final int REQUEST_CODE = 1001;
    public static final int REQUEST_CODE_EXTERNAL_STORAGE = 1002;

    // Hay que iniciar el RecyclerView para mostrar los elementos.
    RecyclerView lv;

    private AudioServiceBinder musicSrv;

    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            musicSrv = binder.getService();
            //musicSrv.setList(songList);
            //musicBound = true;
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

        lv = findViewById (R.id.list1);
        lv.setLayoutManager (new LinearLayoutManager (getBaseContext(), RecyclerView.VERTICAL, false));
        lv.addItemDecoration (new DividerItemDecoration (getBaseContext (), DividerItemDecoration.VERTICAL));

        // Hay que pedir el elemento para cargar los audios.
        // Si no, entonces tendremos un error/choque debido a la estancia de acceso ilegal de archivos.
        int perm = getBaseContext ().checkSelfPermission (Manifest.permission.READ_EXTERNAL_STORAGE);
        if (perm != PackageManager.PERMISSION_GRANTED) {
            requestPermissions (
                    new String [] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    REQUEST_CODE_EXTERNAL_STORAGE
            );
        } else {
            loadAudios ();
        }

    }

    public void songPicked(View view) {
        Log.d("MainActivity","Set Song");
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

        ListadoMusica adapter = new ListadoMusica (getApplicationContext(), artists);
        lv.setAdapter (adapter);
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
                    loadAudios ();
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
