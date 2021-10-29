package player.sazzer;

import android.net.Uri;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String album;
    private Uri albumArt;

    Song(long songID, String songTitle, String songArtist, String songAlbum, Uri albumArt)
    {
        this.id = songID;
        this.title = songTitle;
        this.artist = songArtist;
        this.album = songAlbum;

        if( albumArt != null )
        {
            this.albumArt = albumArt;
        }
    }

    long getId() { return this.id; }
    String getTitle() { return this.title; }
    String getArtist() { return this.artist; }
    String getAlbum() { return this.album; }
    Uri getAlbumArt() { return this.albumArt; }
}
