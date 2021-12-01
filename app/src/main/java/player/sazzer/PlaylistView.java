package player.sazzer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
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
import player.sazzer.DataTypes.Song;

public class PlaylistView extends Activity implements PlaylistRecyclerViewAdapter.ItemClickListener {
    private PlaylistRecyclerViewAdapter adapter;
    private LinearLayoutManager LLM;
    private ArrayList<Song> songs;
    private RecyclerView recyclerView;
    private IntentFilter mIntentFilter;
    private int oldpos = 0;

    public static final String mBroadcasterPlayListView = "player.sazzer.action.PLAYLIST_VIEW";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlistview);

        // Time to setup the area.
        LLM = new LinearLayoutManager(this);
        recyclerView = findViewById(R.id.playlistView);
        recyclerView.setLayoutManager(LLM);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcasterPlayListView);

        this.registerReceiver(playlistDataReciever, mIntentFilter);

        onNewIntent(this.getIntent());
    }

    // Receieve broadcasts from other classes.
    private final BroadcastReceiver playlistDataReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            Log.d("MainActivity","Recieved a braodcast");

            if( extras == null )
                return;

            if( intent.getAction().equals(mBroadcasterPlayListView) )
            {
                Log.d(mBroadcasterPlayListView, "Creating list");
                int position = intent.getIntExtra("position",-1);
                Song track = songs.get(position);
                adapter.updateCurrentSong( track );

                adapter.notifyItemChanged(oldpos);
                adapter.notifyItemChanged(position);

                oldpos = position;
            }
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String newsongsStr = intent.getStringExtra("Audio.SongArray");

        if( newsongsStr != null && !newsongsStr.isEmpty() ) {
            ArrayList<Song> newsongs = MusicHelpers.ConvertJSONToTracks( newsongsStr );

            if( !newsongs.isEmpty() )
            {
                songs = newsongs;

                int position = intent.getIntExtra("position",-1);
                if( position != -1 )
                {
                    oldpos = position;
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
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(playlistDataReciever);
    }

    @Override
    public void onItemClick(View view, int position) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(DetailsActivity.mBroadcasterAudioAction);
        Song track = songs.get(position);
        broadcastIntent.putExtra("songName", track.getTitle());
        broadcastIntent.putExtra("songArtist", track.getArtist());
        broadcastIntent.putExtra("songArt", track.getAlbum().getAlbumArt());
        getApplicationContext().sendBroadcast(broadcastIntent);

        adapter.updateCurrentSong( track );
        adapter.notifyItemChanged(oldpos);
        adapter.notifyItemChanged(position);

        oldpos = position;

        MusicHelpers.actionServicePlaySong(getApplicationContext(), position);
    }
}
