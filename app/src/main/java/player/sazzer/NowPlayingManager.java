package player.sazzer;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.FileNotFoundException;
import java.io.IOException;

public class NowPlayingManager {
    public static final String CHANNEL_ID = "CHANNEL_1";
    public static final String ACTION_NEXT = "NEXT";
    public static final String ACTION_PREV = "PREVIOUS";
    public static final String ACTION_PLAY = "PLAY";

    public static Notification notification;

    private static Bitmap getAlbumImage(String path) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        byte[] data = mmr.getEmbeddedPicture();
        if (data != null) return BitmapFactory.decodeByteArray(data, 0, data.length);
        return null;
    }

    public static void createNotification(Context context, Song track, int pos, int size) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationManagerCompat NMC = NotificationManagerCompat.from(context);
            //MediaSessionCompat MSessionCompat = new MediaSessionCompat( context, "tag" );

            // TODO: Made album art compatible
            // Bitmap icon = BitmapFactory.decodeResource( context.getResources(), track.getAlbumArt() );
            Bitmap bitmap = null;
            //String pathID = track.getAlbumArt();

            //bitmap = getAlbumImage( track.getAlbumArt() );
            try {
                bitmap = MediaStore.Images.Media.getBitmap(
                        context.getContentResolver(), track.getAlbumArt());
                bitmap = Bitmap.createScaledBitmap(bitmap, 30, 30, true);
                bitmap = getAlbumImage( track.getAlbumArt().getPath() );
            } catch (FileNotFoundException exception) {
                exception.printStackTrace();
                bitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.default_cover);
            } catch (IOException e) {
                e.printStackTrace();
            }

            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_play_arrow_black_48dp)
                    .setContentTitle(track.getTitle())
                    .setContentText(track.getArtist())
                    .setContentInfo(track.getAlbum())
                    .setLargeIcon(bitmap)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build();

            NMC.notify(1, notification);
            /*
            NotificationChannel Notif1 = new NotificationChannel(
                    CHANNEL_ID_1, "Channel(1)", NotificationManager.IMPORTANCE_HIGH);

            Notif1.setDescription("Description ofChannel");

            NotificationManager Manager = getSystemService(NotificationManager.class);
            Manager.createNotificationChannel(Notif1);
            */
        }
    }
}
