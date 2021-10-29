package player.sazzer;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import java.io.FileNotFoundException;
import java.io.IOException;

public class NowPlayingManager {
    public static final String CHANNEL_ID = "CHANNEL_1";
    public static final String ACTION_NEXT = "NEXT";
    public static final String ACTION_PREV = "PREVIOUS";
    public static final String ACTION_PLAY = "PLAY";

    private Notification notification;
    private Context parent;

    private RemoteViews remoteView;
    NotificationManagerCompat NMC;

    private static Bitmap getAlbumImage(String path) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        byte[] data = mmr.getEmbeddedPicture();
        if (data != null) return BitmapFactory.decodeByteArray(data, 0, data.length);
        return null;
    }

    public NowPlayingManager(Context context, AudioServiceBinder service) {
        this.parent = context;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NMC = NotificationManagerCompat.from(context);
        }
    }

    public void updateSong( Song track )
    {
        // TODO: Made album art compatible
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(
                    parent.getContentResolver(), track.getAlbumArt());
            bitmap = Bitmap.createScaledBitmap(bitmap, 30, 30, true);
            bitmap = getAlbumImage( track.getAlbumArt().getPath() );
        } catch (FileNotFoundException exception) {
            exception.printStackTrace();
            bitmap = BitmapFactory.decodeResource(parent.getResources(),
                    R.drawable.default_cover);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //remoteView = new RemoteViews(parent.getPackageName(), R.layout.notificationview);

        //setListeners(remoteView);

        // Create an intent that will move to the detailed song info screen.
        Intent intent = new Intent(parent, DetailsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("songName", track.getTitle());
        intent.putExtra("songArtist", track.getArtist());
        PendingIntent showSongIntent = PendingIntent.getActivity(parent, 0, intent, 0);

        notification = new NotificationCompat.Builder(parent, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play_arrow_black_48dp)
                .setOngoing(true)
                .setContentTitle(track.getTitle())
                .setContentText(track.getArtist())
                .setSubText(track.getAlbum())
                .setLargeIcon(bitmap)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentIntent(showSongIntent)
                .setAutoCancel(false)
                .setTicker("something")
                .build();

        NMC.notify(1, notification);
    }

    public void cancelNotification()
    {
        NMC.cancel(2);
    }
}
