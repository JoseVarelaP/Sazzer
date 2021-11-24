package player.sazzer.DataTypes;

import java.util.ArrayList;

public class Album {
    //private final long id;
    private final String title;
    private final String artist;
    private final String albumArt;
    private ArrayList<Song> songs;

    public Album(String albumTitle, String albumArtist ,String albumArt )
    {
        //this.id = albumID;
        this.title = albumTitle;
        this.artist = albumArtist;
        this.albumArt = albumArt;
    }

    public Album(String albumTitle, String albumArtist, String albumArt, ArrayList<Song> songs)
    {
        //this.id = albumID;
        this.title = albumTitle;
        this.artist = albumArtist;
        this.albumArt = albumArt;
        this.songs = songs;
    }

    //long getId() { return this.id; }
    public String getTitle() { return this.title; }
    public String getArtist() { return this.artist; }
    public String getAlbumArt() { return this.albumArt; }
}
