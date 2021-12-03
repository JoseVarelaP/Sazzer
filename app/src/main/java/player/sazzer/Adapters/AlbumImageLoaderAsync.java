package player.sazzer.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;

import player.sazzer.DataTypes.Album;
import player.sazzer.MusicHelpers;

public class AlbumImageLoaderAsync extends AsyncTask<Album, Void, Bitmap> {

    public interface Listener {
        void onImageDownloaded(final Bitmap bitmap);
        void onImageDownloadError();
    }

    private final Listener listener;

    @Override protected Bitmap doInBackground(Album... path) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();

        if( path[0].getGeneratedAlbumArt() != null ) {
            //Log.d("doInBackground", String.format("Album %s already has an image generated.", path[0].getTitle()));
            return path[0].getGeneratedAlbumArt();
        }

        Bitmap coverIMG = MusicHelpers.findAlbumArtCoverFile(path[0].getAlbumArt());
        if( coverIMG != null ) {
            // Save image data on Album object for later use.
            path[0].setGeneratedAlbumArt( MusicHelpers.ResizeBitmap(coverIMG) );
            return MusicHelpers.ResizeBitmap(coverIMG);
        }

        try{
            mmr.setDataSource(path[0].getAlbumArt());
            byte[] data = mmr.getEmbeddedPicture();
            if (data != null) {
                Bitmap finalres = BitmapFactory.decodeByteArray(data, 0, data.length);
                // Save image data on Album object for later use.
                path[0].setGeneratedAlbumArt( MusicHelpers.ResizeBitmap(finalres) );
                return finalres;
            }
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
