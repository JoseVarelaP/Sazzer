package player.sazzer.DataTypes;

import player.sazzer.SongFinder;

public class Song {
    private final long id;
    private final String title;
    private final String artist;
    private final long albumID;
    private final String songPath;
    private final long duration;

    public Song(
            long songID, String songPath, String songTitle, String songArtist, long songAlbum, long duration
    )
    {
        this.id = songID;
        this.title = songTitle;
        this.artist = songArtist;
        this.albumID = songAlbum;
        this.songPath = songPath;
        this.duration = duration;
    }

    long getId() { return this.id; }
    public String getTitle() { return this.title; }
    public String getArtist() { return this.artist; }
    public Album getAlbum() { return SongFinder.GetAlbumFromID( this.albumID ); }
    public String getSongPath() { return songPath; }
    public long getDuration() { return duration; }
}
