package player.sazzer.DataBase;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database( entities = {Song.class}, version = 1)
public abstract class AppDataBase extends RoomDatabase {
    public abstract SongDao songDao ();
}
