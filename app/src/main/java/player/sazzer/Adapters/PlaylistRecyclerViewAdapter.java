package player.sazzer.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import player.sazzer.R;
import player.sazzer.DataTypes.Song;

public class PlaylistRecyclerViewAdapter extends RecyclerView.Adapter<PlaylistRecyclerViewAdapter.ViewHolder> {

    private final ArrayList<Song> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Song current;
    private Context context;

    // data is passed into the constructor
    public PlaylistRecyclerViewAdapter(Context context, ArrayList<Song> data, Song current) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.current = current;
        this.context = context;
    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Song track = mData.get(position);

        holder.playIcon.setVisibility(View.GONE);
        holder.container.setBackgroundColor( ContextCompat.getColor(context,R.color.colorBG)  );
        if( track != null && track == current )
        {
            holder.container.setBackgroundColor( ContextCompat.getColor(context,R.color.colorPrimary)  );
            holder.playIcon.setVisibility(View.VISIBLE);
        }

        if( holder.albumLoaderThread != null )
            if( holder.albumLoaderThread.getStatus() == AsyncTask.Status.RUNNING ) {
                //Log.d("albumLoaderThread","Still Running!");
                holder.albumLoaderThread.cancel(true);
            }

        // Reset thread for a new action.
        holder.albumLoaderThread = new AlbumImageLoaderAsync( holder.albumListener );
        holder.albumLoaderThread.execute(track.getAlbum());

        holder.songName.setText( track.getTitle() );
        holder.songArtist.setText( track.getArtist() );
        holder.songAlbum.setText( track.getAlbum().getTitle() );
        holder.songArt.setImageResource(R.drawable.default_cover);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void updateCurrentSong(Song newSong) { current = newSong; }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView songName,songArtist,songAlbum;
        ImageView songArt,playIcon;
        LinearLayout container;
        AlbumImageLoaderAsync albumLoaderThread;
        AlbumImageLoaderAsync.Listener albumListener;

        ViewHolder(View itemView) {
            super(itemView);
            // Create listener to be able to update the holder's image.
            albumListener = new AlbumImageLoaderAsync.Listener() {
                @Override
                public void onImageDownloaded(Bitmap bitmap) {
                    songArt.setImageBitmap(bitmap);
                }

                @Override
                public void onImageDownloadError() {
                }
            };
            container = itemView.findViewById(R.id.songContainer);
            playIcon = itemView.findViewById(R.id.playIcon);
            songName = itemView.findViewById(R.id.nombreCancion);
            songArtist = itemView.findViewById(R.id.nombreArtista);
            songAlbum = itemView.findViewById(R.id.nombreAlbum);
            songArt = itemView.findViewById(R.id.coverImage);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }
    
    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
