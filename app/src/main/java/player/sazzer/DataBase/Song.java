package player.sazzer.DataBase;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Song {

    public Song() {}

    public Song(String songName, String songArtist, String lyric) {
        this.songArtist = songArtist;
        this.songName = songName;
        this.lyric = lyric;
    }

    @PrimaryKey( autoGenerate = true)
    public int id;

    @ColumnInfo( name = "songName" )
    public String songName;

    @ColumnInfo( name = "songArtist" )
    public String songArtist;

    @ColumnInfo( name = "lyric")
    public String lyric;
}
