package player.sazzer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import player.sazzer.Adapters.PlaylistRecyclerViewAdapter;
import player.sazzer.DataTypes.Song;

public class MainActivity extends AppCompatActivity implements PlaylistRecyclerViewAdapter.ItemClickListener {
    public static final int REQUEST_CODE_EXTERNAL_STORAGE = 1;

    Intent playIntent = null;
    ArrayList<Song> songList;
    private AudioServiceBinder musicSrv;

    private IntentFilter mIntentFilter;

    public static final String mBroadcasterMainActivity = "player.sazzer.action.UPDATE_SONG_VIEW";

    private final ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioServiceBinder.LocalBinder binder = (AudioServiceBinder.LocalBinder) service;
            musicSrv = binder.getService();
            //musicSrv.setCallback(MainActivity.this);
            Log.d("musicConnection","Service has started");
            sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_FETCH_SONGS) );
            sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_OBTAIN_SONGS_TO_DISPLAY) );
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    private void grantPermission(String permission, int requestCode) {
        long checkPermission = getBaseContext().checkSelfPermission(permission);

        if( checkPermission != PackageManager.PERMISSION_GRANTED ) {
            //requestPermissions(new String[]{permission},requestCode);
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_main);

        // Hay que pedir el elemento para cargar los audios.
        // Si no, entonces tendremos un error/choque debido a la estancia de acceso ilegal de archivos.
        grantPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_EXTERNAL_STORAGE);
        //grantPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_CODE_EXTERNAL_STORAGE);

        songList = new ArrayList<>();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcasterMainActivity);

        this.registerReceiver(musicDataReciever, mIntentFilter);

        if (playIntent == null) {
            Log.d("onCreate","Intent is null, starting service.");
            playIntent = new Intent(this, AudioServiceBinder.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
            Log.d("onCreate","Done with setup of services.");
        }
    }

    protected void GenerateMainSongList()
    {
        Log.d("GenerateMainSongList", "Creating view for song items...");
        RecyclerView songView = findViewById(R.id.songList);
        songView.setLayoutManager(new LinearLayoutManager(this));

        Log.d("GenerateMainSongList", "The array contains " + songList.size() + " songs");

        PlaylistRecyclerViewAdapter listMusica = new PlaylistRecyclerViewAdapter(this, songList, null);
        listMusica.setClickListener(this);
        songView.setAdapter(listMusica);
        Log.d("GenerateMainSongList", "Done.");
    }

    @Override
    protected void onStart() {
        Log.d("onStart","Starting");
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv = null;
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            this.unregisterReceiver(musicDataReciever);
        } catch (IllegalArgumentException e)
        {
            // Don't do anything.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onItemClick(View view, int position) {
        Log.d("MainActivity","Set Song: " + position );

        // Broadcast a music list reset to the service.
        sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_CLEAN_QUEUE) );

        sendBroadcast( MusicHelpers.createIntentToUpdateMusicArray(songList) );

        MusicHelpers.actionServicePlaySong(getApplicationContext(), position);

        Intent nt = MusicHelpers.sendToDetailedSongInfo(MainActivity.this, songList.get(position), musicSrv);
        nt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(nt);
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_FETCH_SONGS) );
                sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_OBTAIN_SONGS_TO_DISPLAY) );
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

    // Receieve broadcasts from other classes.
    private final BroadcastReceiver musicDataReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            Log.d("MainActivity","Recieved a broadcast");

            if( extras == null )
                return;

            if( intent.getAction().equals(mBroadcasterMainActivity) )
            {
                Log.d(mBroadcasterMainActivity, "Creating list");
                String newsongsStr = intent.getStringExtra("Audio.SongArray");

                if( newsongsStr != null && !newsongsStr.isEmpty() ) {
                    Log.d(mBroadcasterMainActivity, "List contains data! " + newsongsStr);
                    songList = (ArrayList<Song>) MusicHelpers.ConvertJSONToTracks( newsongsStr );

                    // With song infomation created, we can now generate the song list safely.
                    GenerateMainSongList();
                }
            }
        }
    };
}
