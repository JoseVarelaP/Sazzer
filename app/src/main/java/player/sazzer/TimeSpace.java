package player.sazzer;

import android.annotation.SuppressLint;

import java.util.concurrent.TimeUnit;

class TimeSpace {
    long hours;
    long minute;
    long second;

    public TimeSpace(int seconds)
    {
        hours = TimeUnit.MILLISECONDS.toHours(seconds);
        minute = TimeUnit.MILLISECONDS.toMinutes(seconds) - (TimeUnit.MILLISECONDS.toHours(seconds)* 60);
        second = TimeUnit.MILLISECONDS.toSeconds(seconds) - (TimeUnit.MILLISECONDS.toMinutes(seconds) *60);
    }

    @SuppressLint("DefaultLocale")
    public String convertToReadableMusicTime()
    {
        String time = "";
        if( hours > 0 )
            time += String.format( "%02d:", hours );

        time += String.format( "%02d:%02d", minute, second );

        return time;
    }
}
