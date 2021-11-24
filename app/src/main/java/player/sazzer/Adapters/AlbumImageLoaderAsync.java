package player.sazzer.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;

import player.sazzer.MusicHelpers;

public class AlbumImageLoaderAsync extends AsyncTask<String, Void, Bitmap> {

    public interface Listener {
        void onImageDownloaded(final Bitmap bitmap);
        void onImageDownloadError();
    }

    private final Listener listener;

    @Override protected Bitmap doInBackground(String... path) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();

        Bitmap coverIMG = MusicHelpers.findAlbumArtCoverFile(path[0]);
        if( coverIMG != null ) {
            return MusicHelpers.ResizeBitmap(coverIMG);
        }

        try{
            mmr.setDataSource(path[0]);
            byte[] data = mmr.getEmbeddedPicture();
            if (data != null) return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch ( IllegalArgumentException e ) {
            Log.e("doInBackground", e.toString());
            return null;
        }
        return null;
    }
    @Override protected void onPostExecute(Bitmap result) {
        if( null != result )
        {
            listener.onImageDownloaded(result);
        } else {
            listener.onImageDownloadError();
        }
    }

    public AlbumImageLoaderAsync(final Listener listener)
    {
        this.listener = listener;
    }
}
