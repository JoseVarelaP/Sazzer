package player.sazzer.DataTypes;

public class Song {
    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final String albumArt;
    private final long duration;

    public Song(
            long songID, String songTitle, String songArtist, String songAlbum, String albumArt,
            long duration
    )
    {
        this.id = songID;
        this.title = songTitle;
        this.artist = songArtist;
        this.album = songAlbum;
        this.albumArt = albumArt;
        this.duration = duration;
    }

    long getId() { return this.id; }
    public String getTitle() { return this.title; }
    public String getArtist() { return this.artist; }
    public String getAlbum() { return this.album; }
    public String getAlbumArt() { return this.albumArt; }
    public long getDuration() { return duration; }
}
