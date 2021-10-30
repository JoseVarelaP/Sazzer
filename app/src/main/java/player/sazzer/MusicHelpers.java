package player.sazzer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.ArrayList;

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

    public static Intent quickIntentFromAction(AudioServiceAction action)
    {
        Intent i = new Intent();
        i.setAction(AudioServiceBinder.mBroadcasterServiceBinder);
        i.putExtra("AUDIO_ACTION", action);
        return i;
    }

    /**
     * (Utilizes Gson to convert the array to a serialized string).
     * @param songList New list to send to the service.
     * @return AUDIO_SERVICE_ACTION_UPDATE_BINDER brodcast action.
     */
    public static Intent createIntentToUpdateMusicArray(ArrayList<Song> songList) {
        Gson gson = new Gson();
        String jsonMusica = gson.toJson(songList);

        Intent intent = new Intent();
        intent.setAction(AudioServiceBinder.mBroadcasterServiceBinder);
        intent.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_UPDATE_BINDER);
        intent.putExtra("Audio.SongArray", jsonMusica);

        return intent;
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
