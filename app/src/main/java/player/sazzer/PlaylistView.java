package player.sazzer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import player.sazzer.Adapters.PlaylistRecyclerViewAdapter;

public class PlaylistView extends Activity implements PlaylistRecyclerViewAdapter.ItemClickListener {
    PlaylistRecyclerViewAdapter adapter;
    ArrayList<Song> songs;
    RecyclerView recyclerView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlistview);

        // Time to setup the area.
        recyclerView = findViewById(R.id.playlistView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        onNewIntent(this.getIntent());
    }

    public static final String mPlaylistAudioAction = "player.sazzer.action.UPDATE_PLAYLIST";

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("PlaylistView", "onNewIntent: Starting");

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
                songs = (ArrayList<Song>)newsongs;

                adapter = new PlaylistRecyclerViewAdapter(this, songs);
                adapter.setClickListener(this);
                recyclerView.setAdapter(adapter);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onItemClick(View view, int position) {
        MusicHelpers.actionServicePlaySong(getApplicationContext(), position);
    }
}
