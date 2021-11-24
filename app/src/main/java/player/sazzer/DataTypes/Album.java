package player.sazzer.DataTypes;

import android.graphics.Bitmap;

import java.util.ArrayList;

public class Album {
    private final String title;
    private final String artist;
    private final String albumArt;
    private Bitmap generatedAlbumArt = null;
    private ArrayList<Song> songs;

    public Album(String albumTitle, String albumArtist ,String albumArt )
    {
        this.title = albumTitle;
        this.artist = albumArtist;
        this.albumArt = albumArt;
    }

    public Album(String albumTitle, String albumArtist, String albumArt, ArrayList<Song> songs)
    {
        this.title = albumTitle;
        this.artist = albumArtist;
        this.albumArt = albumArt;
        this.songs = songs;
    }

    public void setGeneratedAlbumArt( Bitmap data ) { this.generatedAlbumArt = data; }
    public Bitmap getGeneratedAlbumArt() { return generatedAlbumArt; }
    public String getTitle() { return this.title; }
    public String getArtist() { return this.artist; }
    public String getAlbumArt() { return this.albumArt; }
}
