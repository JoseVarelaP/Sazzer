package player.sazzer.DataBase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SongDao {
    @Query ("SELECT * FROM song WHERE songName LIKE :title AND songArtist LIKE :artist LIMIT 1")
    Song findByTitleAndArtist(String title, String artist);

    @Insert
    void insertSong(Song song);
}
