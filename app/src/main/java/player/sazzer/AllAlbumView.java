package player.sazzer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import player.sazzer.Adapters.AlbumViewRecyclerViewAdapter;
import player.sazzer.DataTypes.Album;

public class AllAlbumView extends Activity implements AlbumViewRecyclerViewAdapter.ItemClickListener {
    ArrayList<Album> songList;
    private IntentFilter mIntentFilter;
    private LinearLayoutManager LLM;
    private RecyclerView recyclerView;
    private AlbumViewRecyclerViewAdapter adapter;
    public static final String mBroadcasterMainActivity = "player.sazzer.action.UPDATE_SONG_VIEW";

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d("Intent","woooo");

        Log.d(mBroadcasterMainActivity, "Creating list");
        String newsongsStr = intent.getStringExtra("Audio.SongArray");

        if( newsongsStr != null && !newsongsStr.isEmpty() ) {
            Log.d(mBroadcasterMainActivity, "List contains data! " + newsongsStr);
            songList = MusicHelpers.ConvertJSONToAlbums( newsongsStr );

            // With song infomation created, we can now generate the song list safely.
            adapter = new AlbumViewRecyclerViewAdapter(this, songList);
            adapter.setClickListener(this);
            recyclerView.setAdapter(adapter);
            //GenerateMainSongList();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_albumview);

        LLM = new LinearLayoutManager(this);
        recyclerView = findViewById(R.id.playlistView);
        recyclerView.setLayoutManager(LLM);

        //songList = new ArrayList<>();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcasterMainActivity);
        this.registerReceiver(musicDataReciever, mIntentFilter);

        onNewIntent(this.getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
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
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults);
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
            /*
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
                    songList = MusicHelpers.ConvertJSONToAlbums( newsongsStr );
                    //MusicHelpers.ConvertJSONToDynamicList( songList, newsongsStr );

                    // With song infomation created, we can now generate the song list safely.
                    GenerateMainSongList();
                }
            }
             */
        }
    };
}
