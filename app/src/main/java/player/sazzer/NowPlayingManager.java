package player.sazzer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
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
    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_PREV = "PREV";
    public static final String ACTION_NEXT = "NEXT";

    private Bitmap image;
    MediaSessionCompat mediaSessionCompat;
    NotificationManagerCompat NMC;
    Song track;
    int draw_prev,draw_play,draw_next;

    private PlaybackStateCompat.Builder mStateBuilder;

    public NowPlayingManager() {}

    private void createNotificationChannel() {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel nCH1 = new NotificationChannel(CHANNEL_ID,
                    "Music Playback", NotificationManager.IMPORTANCE_LOW);
            nCH1.setDescription("Manager for music playback");
            // Register the notification to the application so the system can acknowledge it.
            NotificationManager NManager = getSystemService(NotificationManager.class);
            NManager.createNotificationChannel(nCH1);

            Log.d("createNotificationChannel", "Done.");
        }
    }

    public void onCreate() {
        super.onCreate();
        Log.d("NowPlayingManager","Creating");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NMC = NotificationManagerCompat.from(this);
        }
        createNotificationChannel();

        draw_prev = R.drawable.ic_baseline_skip_previous_24;
        draw_play = R.drawable.ic_play_white_48dp;
        draw_next = R.drawable.ic_baseline_skip_next_24;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void updateMediaSessionPosition(long pos)
    {
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(PlaybackStateCompat.STATE_PLAYING,pos,1, SystemClock.elapsedRealtime());
        mediaSessionCompat.setPlaybackState(mStateBuilder.build());
    }

    public void updateSong(Song track, boolean forceAlbumImageRegen )
    {
        this.track = track;

        if( (image == null || forceAlbumImageRegen) )
            image = MusicHelpers.getAlbumImage( this.track.getAlbumArt() );

        if( image == null )
            image = BitmapFactory.decodeResource(getResources(),R.drawable.default_cover);

        if( mediaSessionCompat == null)
            mediaSessionCompat = new MediaSessionCompat(this, "PlayerAudio");

        if( mStateBuilder == null ) {
            mStateBuilder = new PlaybackStateCompat.Builder()
                    .setActions(
                            PlaybackStateCompat.ACTION_PLAY |
                                    PlaybackStateCompat.ACTION_PAUSE |
                                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                    PlaybackStateCompat.ACTION_SEEK_TO
                    );
            mediaSessionCompat.setPlaybackState(mStateBuilder.build());
        }

        mediaSessionCompat.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, image)
                .putString(
                        MediaMetadataCompat.METADATA_KEY_ARTIST,
                        this.track.getArtist()
                )
                .putString(
                        MediaMetadataCompat.METADATA_KEY_ALBUM,
                        this.track.getAlbum()
                )
                .putString(
                        MediaMetadataCompat.METADATA_KEY_TITLE,
                        this.track.getTitle()
                )
                .putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        this.track.getDuration()
                )
                .build()
        );

        mediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
                @Override
                public void onSeekTo(long pos) {
                    super.onSeekTo(pos);

                    Intent forThePlayer = new Intent();
                    forThePlayer.setAction(AudioServiceBinder.mBroadcasterServiceBinder);
                    forThePlayer.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_UPDATE_PROGRESS);
                    forThePlayer.putExtra("Audio.SeekProgress", (int)pos );
                    sendBroadcast(forThePlayer);
                }
            }
        );
        mediaSessionCompat.setActive(true);

        showNotification();
    }

    public void showNotification()
    {
        // Common flag for all intents.
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        Intent intentPrevious = new Intent(this, NotificationReceiver.class).setAction(ACTION_PREV);
        PendingIntent PIPrevious = PendingIntent.getBroadcast(this, 0, intentPrevious, flags);

        Intent intentPlay = new Intent(this, NotificationReceiver.class).setAction(ACTION_PLAY);
        PendingIntent PIPlay = PendingIntent.getBroadcast(this, 0, intentPlay, flags);

        Intent intentNext = new Intent(this, NotificationReceiver.class).setAction(ACTION_NEXT);
        PendingIntent PINext = PendingIntent.getBroadcast(this, 0, intentNext, flags);

        // Create an intent that will move to the detailed song info screen.
        Intent intent = MusicHelpers.sendToDetailedSongInfo(this, track, null);

        //Log.d("NowPlayingManager",String.format("Created a new intent with the following data: %s by %s", track.getTitle(), track.getArtist()));
        PendingIntent showSongIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(showSongIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(draw_prev, "Previous", PIPrevious)
                .addAction(draw_play, "Play", PIPlay)
                .addAction(draw_next, "Next", PINext)
                .setStyle( new androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0,1,2)
                    .setMediaSession(mediaSessionCompat.getSessionToken()
                    )
                )
                .setSmallIcon(R.mipmap.ic_launcher);

        NotificationManagerCompat.from(this).notify(0, builder.build());
    }

    public void setPauseIcon(boolean isPlaying, long curTime)
    {
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        if(isPlaying)
        {
            draw_play = R.drawable.ic_pause_white_48dp;
        } else {
            draw_play = R.drawable.ic_play_white_48dp;
        }

        if( mStateBuilder != null )
            mStateBuilder.setState( state, curTime, 1, SystemClock.elapsedRealtime() );

        showNotification();
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
