package player.sazzer;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import player.sazzer.DataTypes.Song;

public class NowPlayingManager extends Activity {
    public static final String CHANNEL_ID = "CHANNEL_1";

    private Notification notification;
    private final Context parent;
    private Bitmap image;
    MediaSessionCompat mediaSessionCompat;
    NotificationManagerCompat NMC;

    private PlaybackStateCompat.Builder mStateBuilder;

    public NowPlayingManager(Context context) {
        this.parent = context;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NMC = NotificationManagerCompat.from(context);
        }
    }

    public void updateSong(Song track, int percentage, AudioServiceBinder bind, boolean forceAlbumImageRegen )
    {

        if( parent == null )
            return;

        if( (image == null || forceAlbumImageRegen) )
            image = MusicHelpers.getAlbumImage( track.getAlbumArt() );

        if( image == null )
            image = BitmapFactory.decodeResource(bind.getResources(),R.drawable.default_cover);

        int draw_prev,draw_play,draw_next;

        if( mediaSessionCompat == null)
            mediaSessionCompat = new MediaSessionCompat(parent, "tag");

        if( mStateBuilder == null ) {
            mStateBuilder = new PlaybackStateCompat.Builder()
                    .setActions(
                            PlaybackStateCompat.ACTION_PLAY |
                                    PlaybackStateCompat.ACTION_PAUSE |
                                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                    );
            mediaSessionCompat.setPlaybackState(mStateBuilder.build());
        }
        mediaSessionCompat.setActive(true);

        draw_prev = R.drawable.ic_baseline_skip_previous_24;
        Intent intentPrevious = MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_PREV_SONG);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent PIPrevious = PendingIntent.getBroadcast(parent, 0, intentPrevious, flags);

        draw_play = R.drawable.ic_play_white_48dp;
        Intent intentPlay = MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_TOGGLE_PLAY);
        PendingIntent PIPlay = PendingIntent.getBroadcast(parent, 0, intentPlay, flags);

        draw_next = R.drawable.ic_baseline_skip_next_24;
        Intent intentNext = MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_NEXT_SONG);
        PendingIntent PINext = PendingIntent.getBroadcast(parent, 0, intentNext, flags);

        // Create an intent that will move to the detailed song info screen.
        Intent intent = MusicHelpers.sendToDetailedSongInfo(parent, track, bind);

        //Log.d("NowPlayingManager",String.format("Created a new intent with the following data: %s by %s", track.getTitle(), track.getArtist()));
        PendingIntent showSongIntent = PendingIntent.getActivity(parent, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        notification = new NotificationCompat.Builder(parent, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play_white_48dp)
                .setContentTitle(track.getTitle())
                .setContentText(track.getArtist())
                .setLargeIcon(image)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setProgress( intent.getIntExtra("TotalTime", 0) , intent.getIntExtra("Progress", 0), false )
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(false)
                .setContentIntent(showSongIntent)
                .addAction(draw_prev, "Previous", PIPrevious)
                .addAction(draw_play, "Play", PIPlay)
                .addAction(draw_next, "Next", PINext)
                .setStyle( new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0,1,2)
                        .setMediaSession(mediaSessionCompat.getSessionToken())
                )
                .build();

        NMC.notify(1, notification);
    }

    public void cancelNotification()
    {
        NMC.cancel(1);
    }
}
