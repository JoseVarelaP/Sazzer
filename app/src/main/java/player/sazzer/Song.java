package player.sazzer;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String album;

    Song(long songID, String songTitle, String songArtist, String songAlbum)
    {
        this.id = songID;
        this.title = songTitle;
        this.artist = songArtist;
        this.album = songAlbum;
    }

    long getId() { return this.id; }
    String getTitle() { return this.title; }
    String getArtist() { return this.artist; }
    String getAlbum() { return this.album; }
}
