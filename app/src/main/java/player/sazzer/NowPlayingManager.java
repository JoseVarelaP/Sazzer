package player.sazzer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import player.sazzer.DataTypes.Song;

public class NowPlayingManager extends Service {
    public static final String CHANNEL_ID = "CHANNEL_1";
    public static final String mBroadcasterNotificationAction = "player.sazzer.action.UPDATE_NOTIFICATION";

    private Notification notification;
    //private final Context parent;
    private Bitmap image;
    MediaSessionCompat mediaSessionCompat;
    NotificationManagerCompat NMC;
    private IntentFilter mIntentFilter;

    private PlaybackStateCompat.Builder mStateBuilder;

    public NowPlayingManager() {}

    private void createNotificationChannel() {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel nCH1 = new NotificationChannel(CHANNEL_ID,
                    "Music Playback", NotificationManager.IMPORTANCE_HIGH);
            nCH1.setDescription("Manager for music playback");
            // Register the notification to the application so the system can acknowledge it.
            NotificationManager NManager = getSystemService(NotificationManager.class);
            NManager.createNotificationChannel(nCH1);

            Log.d("createNotificationChannel", "Done.");
        }
    }

    // Receieve broadcasts from other classes.
    private final BroadcastReceiver notificationReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            Log.d("notificationReciever","Recieved a braodcast");

            if( extras == null )
                return;

            if( intent.getAction().equals(mBroadcasterNotificationAction) )
            {
                Log.d(mBroadcasterNotificationAction, "Creating list");
                String songStr = intent.getStringExtra("currentSong");
                if( songStr != null && !songStr.isEmpty() ) {
                    Log.d(mBroadcasterNotificationAction, "Found Song Entry.");
                    Song track = MusicHelpers.ConvertJSONToSong(songStr);
                    updateSong( track, true );
                }
            }
        }
    };

    public void onCreate() {
        super.onCreate();
        Log.d("NowPlayingManager","Creating");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NMC = NotificationManagerCompat.from(this);
        }
        createNotificationChannel();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcasterNotificationAction);

        this.registerReceiver(notificationReciever,mIntentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(notificationReciever);
    }

    public void updateSong(Song track, boolean forceAlbumImageRegen )
    {
        if( (image == null || forceAlbumImageRegen) )
            image = MusicHelpers.getAlbumImage( track.getAlbumArt() );

        if( image == null )
            image = BitmapFactory.decodeResource(getResources(),R.drawable.default_cover);

        int draw_prev,draw_play,draw_next;

        if( mediaSessionCompat == null)
            mediaSessionCompat = new MediaSessionCompat(this, "PlayerAudio");

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
        PendingIntent PIPrevious = PendingIntent.getBroadcast(this, 0, intentPrevious, flags);

        draw_play = R.drawable.ic_play_white_48dp;
        Intent intentPlay = MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_TOGGLE_PLAY);
        PendingIntent PIPlay = PendingIntent.getBroadcast(this, 0, intentPlay, flags);

        draw_next = R.drawable.ic_baseline_skip_next_24;
        Intent intentNext = MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_NEXT_SONG);
        PendingIntent PINext = PendingIntent.getBroadcast(this, 0, intentNext, flags);

        // Create an intent that will move to the detailed song info screen.
        Intent intent = MusicHelpers.sendToDetailedSongInfo(this, track, null);

        //Log.d("NowPlayingManager",String.format("Created a new intent with the following data: %s by %s", track.getTitle(), track.getArtist()));
        PendingIntent showSongIntent = PendingIntent.getActivity(this, 0, intent, 0);

        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
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

        //NMC.notify(1, notification);

        NotificationManager nManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        nManager.notify(0, notification);
    }

    public void cancelNotification()
    {
        NMC.cancel(1);
    }

    private final IBinder mBinder = new NowPlayingManager.LocalBinder();
    public class LocalBinder extends Binder {
        public NowPlayingManager getService() {
            return NowPlayingManager.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
