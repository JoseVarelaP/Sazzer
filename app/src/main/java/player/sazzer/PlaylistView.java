package player.sazzer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

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
    LinearLayoutManager LLM;
    ArrayList<Song> songs;
    RecyclerView recyclerView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlistview);

        // Time to setup the area.
        LLM = new LinearLayoutManager(this);
        recyclerView = findViewById(R.id.playlistView);
        recyclerView.setLayoutManager(LLM);

        onNewIntent(this.getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String newsongsStr = intent.getStringExtra("Audio.SongArray");

        if( newsongsStr != null && !newsongsStr.isEmpty() ) {
            Gson gson = new Gson();

            Type type = new TypeToken<List<Song>>(){}.getType();
            List<Song> newsongs = gson.fromJson(newsongsStr, type);

            if( !newsongs.isEmpty() )
            {
                songs = (ArrayList<Song>)newsongs;

                int position = intent.getIntExtra("position",-1);
                if( position != -1 )
                {
                    adapter = new PlaylistRecyclerViewAdapter(this, songs, songs.get(position));
                    adapter.setClickListener(this);
                    recyclerView.setAdapter(adapter);
                    LLM.scrollToPositionWithOffset(position, 180);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onItemClick(View view, int position) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(DetailsActivity.mBroadcasterAudioAction);
        Song track = songs.get(position);
        broadcastIntent.putExtra("songName", track.getTitle());
        broadcastIntent.putExtra("songArtist", track.getArtist());
        broadcastIntent.putExtra("songArt", track.getAlbumArt());
        getApplicationContext().sendBroadcast(broadcastIntent);

        MusicHelpers.actionServicePlaySong(getApplicationContext(), position);
    }
}
