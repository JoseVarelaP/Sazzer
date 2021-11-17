package player.sazzer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class SongFinder extends AppCompatActivity {

    private ArrayList<Song> masterSongList;
    private Context mContext;
    Uri musicUri;

    public SongFinder( Context ctx, Uri man )
    {
        Log.d("SongFinder", "Creating..");
        masterSongList = new ArrayList<>();
        this.musicUri = man;
        this.mContext = ctx;
    }

    public ArrayList<Song> FindMusicByArtist( String artistName )
    {
        ArrayList<Song> result = new ArrayList<>();
        for( Song s : masterSongList )
        {
            if( s.getArtist().contains(artistName) )
                result.add(s);
        }

        return result;
    }

    public ArrayList<Song> FindMusicByAlbumName( String albumName )
    {
        ArrayList<Song> result = new ArrayList<>();
        for( Song s : masterSongList )
        {
            if( s.getArtist().contains(albumName) )
                result.add(s);
        }

        return result;
    }

    // TODO: Make these functions:
    // GetAlbumsFromArtist()
    // LookupSongsFromDirectoryArray()

    public void GenerateSongList()
    {
        Log.d("GenerateSongList", "Creating list");
        ContentResolver musicResolver = mContext.getContentResolver();

        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.ALBUM + " ASC, " + MediaStore.Audio.Media.TRACK + " ASC";

        Cursor musicCursor = musicResolver.query(musicUri, null, selection, null, sortOrder);

        // Hora de buscar
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            //int albumId = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int column_index = musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String pathId = musicCursor.getString(column_index);

                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                masterSongList.add(new Song(thisId, thisTitle, thisArtist, thisAlbum, pathId));
            }
            while (musicCursor.moveToNext());
        }

        // Needs to be closed to avoid memory leak.
        if(musicCursor != null)
            musicCursor.close();
        Log.d("GenerateSongList", "Done. Found " + masterSongList.size() + "songs.");
    }

    public void clearQueue() { masterSongList.clear(); }
    public ArrayList<Song> getList() { return masterSongList; }
}
