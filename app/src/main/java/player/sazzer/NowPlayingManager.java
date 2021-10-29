package player.sazzer;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

public class NowPlayingManager implements Serializable {
    public static final String CHANNEL_ID = "CHANNEL_1";

    private Notification notification;
    private final Context parent;
    NotificationManagerCompat NMC;

    public NowPlayingManager(Context context) {
        this.parent = context;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NMC = NotificationManagerCompat.from(context);
        }
    }

    public void updateSong( Song track, int percentage, AudioServiceBinder bind )
    {
        Bitmap bitmap = MusicHelpers.getAlbumImage( track.getAlbumArt() );

        // Create an intent that will move to the detailed song info screen.
        Intent intent = MusicHelpers.sendToDetailedSongInfo(parent, track, bind);

        //Log.d("NowPlayingManager",String.format("Created a new intent with the following data: %s by %s", track.getTitle(), track.getArtist()));
        PendingIntent showSongIntent = PendingIntent.getActivity(parent, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        notification = new NotificationCompat.Builder(parent, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play_arrow_black_48dp)
                .setOngoing(true)
                .setContentTitle(track.getTitle())
                .setContentText(track.getArtist())
                .setSubText(track.getAlbum() + " " + percentage)
                .setLargeIcon(bitmap)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setSilent(true)
                .setProgress( intent.getIntExtra("TotalTime", 0) , intent.getIntExtra("Progress", 0), false )
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(showSongIntent)
                .setAutoCancel(false)
                .setTicker("something")
                .build();

        NMC.notify(1, notification);
    }

    public void cancelNotification()
    {
        NMC.cancel(1);
    }
}
