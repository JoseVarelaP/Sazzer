package player.sazzer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MusicHelpers {
    /**
     * Generate a Bitmap object that comes from the song's embedded metadata.
     * @param path Path to the song, which contains embedded information
     * @return A generated Bitmap. However, keep in mind that this can be null.
     */
    public static Bitmap getAlbumImage(String path) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        byte[] data = mmr.getEmbeddedPicture();
        if (data != null) return BitmapFactory.decodeByteArray(data, 0, data.length);
        return null;
    }

    /**
     * Creates an intent that will send the user to the DetailsActivity, responsible
     * for showing more in-depth song information.
     * @param context The current context that will be stacked to the chain.
     * @param track Current song that will be used to fill information
     * @return The intent to be used on a related PendingIntent.
     */
    public static Intent sendToDetailedSongInfo(Context context, @NonNull Song track, @Nullable AudioServiceBinder binder)
    {
        Intent intent = new Intent(context, DetailsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("songName", track.getTitle());
        intent.putExtra("songArtist", track.getArtist());
        intent.putExtra("songArt", track.getAlbumArt());

        if( binder != null )
        {
            intent.putExtra("Progress", binder.getCurrentAudioPosition());
            intent.putExtra("TotalTime", binder.getTotalAudioDuration());
        }

        return intent;
    }
}
