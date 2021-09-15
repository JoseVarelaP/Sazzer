package player.sazzer;

public class Song {
    private long id;
    private String title;
    private String artist;

    Song(long songID, String songTitle, String songArtist)
    {
        this.id = songID;
        this.title = songTitle;
        this.artist = songArtist;
    }

    long getId() { return this.id; }
    String getTitle() { return this.title; }
    String getArtist() { return this.artist; }
}
