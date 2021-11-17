package player.sazzer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import player.sazzer.DataTypes.Song;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String mBroadcasterNotificationAction = "player.sazzer.action.UPDATE_NOTIFICATION";
    public static final String NOTIFICATION_TOGGLE_PLAY = "PLAY";
    public static final String NOTIFICATION_TOGGLE_PREV = "PREV";
    public static final String NOTIFICATION_TOGGLE_NEXT = "NEXT";
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        Log.d("notificationReciever","Recieved a braodcast");

        if( extras == null )
            return;

        if( intent.getAction() == null )
            return;


        //Intent intent1 = new Intent(context, AudioServiceBinder.class);

        switch (intent.getAction())
        {
            case NOTIFICATION_TOGGLE_PLAY: {
                Toast.makeText(context, "Play", Toast.LENGTH_SHORT).show();
                context.startService( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_TOGGLE_PLAY) );
                break;
            }
            case NOTIFICATION_TOGGLE_NEXT:
            {
                Toast.makeText(context, "Next", Toast.LENGTH_SHORT).show();
                context.startService( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_NEXT_SONG) );
                break;
            }
            case NOTIFICATION_TOGGLE_PREV:
            {
                Toast.makeText(context, "Prev", Toast.LENGTH_SHORT).show();
                context.startService( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_PREV_SONG) );
                break;
            }
            case mBroadcasterNotificationAction:
            {
                Log.d(mBroadcasterNotificationAction, "Creating list");
                String songStr = intent.getStringExtra("currentSong");
                if( songStr != null && !songStr.isEmpty() ) {
                    Log.d(mBroadcasterNotificationAction, "Found Song Entry.");
                    Song track = MusicHelpers.ConvertJSONToSong(songStr);
                    //updateSong( track, true );
                }
                break;
            }
            default:
                break;
        }
    }
}
