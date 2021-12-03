package player.sazzer.Adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import java.util.Vector;

import player.sazzer.DataTypes.Album;
import player.sazzer.MusicHelpers;
import player.sazzer.R;

public class AlbumImageLoaderAsync extends AsyncTask<Album, Void, Bitmap> {
    public Bitmap dummyIMG = null;
    public Context context = MusicHelpers.getAppContext();

    public interface Listener {
        //Bitmap dummyImage = null;
        void onImageDownloaded(final Bitmap bitmap);
        void onImageDownloadError();

        public default Bitmap getDummyImage() { return null; }
    }

    private final Listener listener;

    @Override protected Bitmap doInBackground(Album... path) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();

        if( dummyIMG == null )
        {
            //Drawable dum = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_launcher_foreground, null);
            dummyIMG = MusicHelpers.getBitmapFromVectorDrawable( R.drawable.ic_launcher_foreground );
            //dummyIMG = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_launcher_foreground, null);
            //dummyIMG = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_foreground);
        }

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

            //Log.d("MediaSource","Not Found, using fallback");

            // Oh noes, no data was found, we'll have to fill it up with dummy data.
            path[0].setGeneratedAlbumArt(dummyIMG);
            return dummyIMG;
        } catch ( IllegalArgumentException e ) {
            Log.e("doInBackground", e.toString());
            // Oh noes, no data was found, we'll have to fill it up with dummy data.
            path[0].setGeneratedAlbumArt(dummyIMG);
            return dummyIMG;
        }
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
