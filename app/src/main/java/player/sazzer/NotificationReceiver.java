package player.sazzer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import player.sazzer.DataTypes.Song;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_PREV = "PREV";
    public static final String ACTION_NEXT = "NEXT";
    @Override
    public void onReceive(Context context, Intent intent) {
        //Bundle extras = intent.getExtras();
        //Log.d("notificationReciever","Recieved a braodcast");
        //Log.d("notificationReciever", "Action: " + (intent.getAction()));

        if( intent.getAction() == null )
            return;

        Intent intent1 = new Intent(context, AudioServiceBinder.class);

        switch (intent.getAction())
        {
            case ACTION_PLAY: {
                intent1.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_TOGGLE_PLAY );
                context.startService(intent1);
                break;
            }
            case ACTION_NEXT:
            {
                intent1.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_NEXT_SONG );
                context.startService(intent1);
                break;
            }
            case ACTION_PREV:
            {
                intent1.putExtra("AUDIO_ACTION", AudioServiceAction.AUDIO_SERVICE_ACTION_PREV_SONG );
                context.startService(intent1);
                break;
            }
            default:
                break;
        }
    }
}
