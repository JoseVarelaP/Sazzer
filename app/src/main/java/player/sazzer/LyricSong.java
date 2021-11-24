package player.sazzer;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LyricSong {
    public static final String API_KEY_LABEL = "apikey=";
    public static final String API_KEY = "29aeabb2b877f655784dbbf82592d33d";
    public static final String METHOD = "matcher.lyrics.get";
    public static final String BASE_URL = "https://api.musixmatch.com/ws/1.1/";
    public static final String FORMAT = "?format=json&callback=callback";
    public static final String ARTIST_LABEL = "q_artist=";
    public static final String TRACK_LABEL = "q_track=";

    private String track_name;
    private String artist;

    public LyricSong(String track_name, String artist){
        this.track_name = track_name;
        this.artist = artist;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTrack_name() {
        return track_name;
    }

    public void setTrack_name(String track_name) {
        this.track_name = track_name;
    }

    private String buildFinalUrl() {
        return BASE_URL + METHOD + FORMAT +
                String.format("&%s%s", TRACK_LABEL, track_name.replace(" ", "%20")) +
                String.format("&%s%s", ARTIST_LABEL, artist.replace(" ", "%20")) +
                String.format("&%s%s", API_KEY_LABEL, API_KEY);
    }

    public String getTrackLyric() {
        try {
            URL url = new URL(buildFinalUrl());
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");

            InputStreamReader isr = new InputStreamReader(httpURLConnection.getInputStream());

            BufferedReader br = new BufferedReader(isr);

            String line;
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }

            JSONObject object = new JSONObject(stringBuilder.toString());
            JSONObject message = object.getJSONObject("message");

            if (message.getJSONObject("header").getInt("status_code") != 200) {
                // System.out.println("Error");
                return null;
            }
            JSONObject object2 = message.getJSONObject("body").getJSONObject("lyrics");

            br.close();
            isr.close();
            httpURLConnection.disconnect();

            return object2.getString("lyrics_body");
        } catch (IOException | JSONException e) {
            return null;
        }
    }
}

class DownloadLyrics extends Thread {
    DetailsActivity context;
    LyricSong lyricSong;

    public DownloadLyrics(DetailsActivity context, LyricSong lyricSong) {
        this.context = context;
        this.lyricSong = lyricSong;
    }

    @Override
    public void run() {
        String ly_temp = lyricSong.getTrackLyric();
        context.runOnUiThread(()-> {
            if (ly_temp == null) {
                context.lyric.setText("");
                context.lyricContainer.setVisibility(View.INVISIBLE);
                Toast.makeText(context, context.getString(R.string.lyricsNotAvailable), Toast.LENGTH_LONG).show();
                return;
            }
            if (ly_temp.equals("")) {
                context.lyric.setText("");
                context.lyricContainer.setVisibility(View.INVISIBLE);
                Toast.makeText(context, context.getString(R.string.forbiddenLyrics), Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(context, context.getString(R.string.successfulDownload), Toast.LENGTH_LONG).show();
            context.lyric.setText(ly_temp);
        });
    }
}