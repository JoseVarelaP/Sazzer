package player.sazzer;

import android.net.Uri;

public class Song {
    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final String albumArt;

    Song(long songID, String songTitle, String songArtist, String songAlbum, String albumArt)
    {
        this.id = songID;
        this.title = songTitle;
        this.artist = songArtist;
        this.album = songAlbum;
        this.albumArt = albumArt;

    }

    long getId() { return this.id; }
    String getTitle() { return this.title; }
    String getArtist() { return this.artist; }
    String getAlbum() { return this.album; }
    String getAlbumArt() { return this.albumArt; }
}
