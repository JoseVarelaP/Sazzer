package player.sazzer;

import android.net.Uri;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String album;
    private String albumArt;

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
