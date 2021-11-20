package player.sazzer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import player.sazzer.DataTypes.Song;

public class MusicHelpers {

    static public class AlbumImageLoaderAsync extends AsyncTask<String, Void, Bitmap> {

        public interface Listener {
            void onImageDownloaded(final Bitmap bitmap);
            void onImageDownloadError();
        }

        private final Listener listener;

        @Override protected Bitmap doInBackground(String... path) {

            android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try{
                mmr.setDataSource(path[0]);
                byte[] data = mmr.getEmbeddedPicture();
                if (data != null) return BitmapFactory.decodeByteArray(data, 0, data.length);
            } catch ( IllegalArgumentException e ) {
                Log.e("doInBackground", e.toString());
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

    /**
     * Verify if the current audio file has a valid audio format supported by the device.
     * @param path Full song path to the audio file.
     * @return {@link Boolean} representing the result.
     */
    public static boolean isSongValidAudio(String path) {
        List<String> availableFormats = Arrays.asList("mp3","ogg","flac","wav","ogv");
        String lowerPath = path.toLowerCase();
        for( String s : availableFormats ) {
            if (lowerPath.endsWith(s))
                return true;
        }

        return false;
    }

    /**
     * Generate a {@link Bitmap} object that comes from the song's embedded metadata.
     * @param path Path to the song, which contains embedded information
     * @return A generated {@link Bitmap}. However, keep in mind that this can be null.
     */
    public static Bitmap getAlbumImage(String path) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try{
            mmr.setDataSource(path);
            byte[] data = mmr.getEmbeddedPicture();
            if (data != null) return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch ( IllegalArgumentException e ) {
            Log.e("getAlbumImage", e.toString());
        }
        return null;
    }

	public static Bitmap ResizeBitmap(Bitmap image) {
        if (image.getWidth() > 600 && image.getHeight() > 600)
            return Bitmap.createScaledBitmap(image, 600, 600, false);
        return image;
    }

    public static Intent quickIntentFromAction(AudioServiceAction action)
    {
        Intent i = new Intent();
        i.setAction(AudioServiceBinder.mBroadcasterServiceBinder);
        i.putExtra("AUDIO_ACTION", action);
        return i;
    }

    /**
     * Notifies the {@link AudioServiceBinder} service to update the contents of its song list
     * with a new set.
     * @param songList New list to send to the service.
     * @return {@link AudioServiceAction#AUDIO_SERVICE_ACTION_UPDATE_BINDER} broadcast action.
     *
     * @see #ConvertSongsToJSONTable(ArrayList)
     */
    public static Intent createIntentToUpdateMusicArray(ArrayList<Song> songList) {
        Intent intent = new Intent();
        intent.setAction(AudioServiceBinder.mBroadcasterServiceBinder);
        intent.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_UPDATE_BINDER);
        intent.putExtra("Audio.SongArray", ConvertSongsToJSONTable(songList));

        return intent;
    }

    /**
     * Creates an intent that will send the user to the DetailsActivity, responsible
     * for showing more in-depth song information.
     * @param context The current context that will be stacked to the chain.
     * @param track Current song that will be used to fill information
     * @return The {@link android.content.Intent} to be used on a related {@link android.app.PendingIntent}.
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

    public static void actionServicePlaySong( Context context, int position )
    {
        Intent intent = new Intent();
        intent.setAction(AudioServiceBinder.mBroadcasterServiceBinder);
        intent.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_UPDATE_SONG_ID);
        intent.putExtra("Audio.SongID", position);
        context.sendBroadcast(intent);
        context.sendBroadcast( quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_PLAY_SONG) );
    }

    /**
     * As a shortcut, we can just use {@link Gson} to convert the ArrayList to a JSON.
     * @param tracks {@link ArrayList} of songs to convert.
     * @return {@link String}
     */
    public static String ConvertSongsToJSONTable(@NonNull ArrayList<Song> tracks)
    {
        Gson gson = new Gson();
        return gson.toJson(tracks);
    }

    public static String ConvertSongsToJSONTable(@NonNull Song tracks)
    {
        Gson gson = new Gson();
        return gson.toJson(tracks);
    }

    /**
     * @param JSONData Stringyfied version of the Song.
     * @return a {@link Song} object.
     *
     * @see #ConvertSongsToJSONTable(ArrayList)
     * @see #ConvertJSONToTracks(String)
     * @see Gson
     */
    public static Song ConvertJSONToSong(@NonNull String JSONData)
    {
        if( JSONData.isEmpty() )
            return null;

        Gson gson = new Gson();
        Type type = new TypeToken<Song>(){}.getType();

        return gson.fromJson(JSONData, type);
    }

    /**
     *
     * @param JSONData Stringyfied version of the Array.
     * @return a {@link List} containing the songs. Can be casted directly into a {@link ArrayList}.
     * 
     * @see #ConvertSongsToJSONTable(ArrayList)
     * @see Gson
     */
    public static List<Song> ConvertJSONToTracks(@NonNull String JSONData)
    {
        if( JSONData.isEmpty() )
            return null;

        Gson gson = new Gson();
        Type type = new TypeToken<List<Song>>(){}.getType();

        return gson.fromJson(JSONData, type);
    }

    public static Intent sendToPlaylist(Context context, @NonNull ArrayList<Song> tracks)
    {
        Intent intent = new Intent(context, PlaylistView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("Audio.SongArray", ConvertSongsToJSONTable(tracks) );
        return intent;
    }
}
