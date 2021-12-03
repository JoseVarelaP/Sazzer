package player.sazzer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import player.sazzer.DataTypes.Album;
import player.sazzer.DataTypes.Song;

public class SongFinder extends AppCompatActivity {

    private static ArrayList<Song> masterSongList;
    private static HashMap<Long, Album> masterAlbumList;
    private Context mContext;
    Uri musicUri;

    public SongFinder( Context ctx, Uri man )
    {
        Log.d("SongFinder", "Creating..");
        masterSongList = new ArrayList<>();
        masterAlbumList = new HashMap<>();
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

    public Album GetAlbumFromName(String albumName )
    {
        return masterAlbumList.get(albumName);
    }

    public ArrayList<Album> getAlbumsFromArtist(String artistName )
    {
        ArrayList<Album> albums = new ArrayList<>();
        for(Map.Entry<Long, Album> alb : masterAlbumList.entrySet() )
        {
            if( alb.getValue().getArtist().equals(artistName) )
                albums.add(alb.getValue());
        }
        return albums;
    }

    public ArrayList<Song> FindMusicByAlbumName( String albumName )
    {
        ArrayList<Song> result = new ArrayList<>();
        for( Song s : masterSongList )
        {
            if( s.getAlbum().getTitle().contains(albumName) )
                result.add(s);
        }

        return result;
    }

    public static Album GetAlbumFromID(long ID)
    {
        return masterAlbumList.get(ID);
    }

    // TODO: Make these functions:
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
            int albumId = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int column_index = musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int durationColumn = musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String pathId = musicCursor.getString(column_index);
                long albumID = musicCursor.getLong(albumId);

                //Log.d("albumColumn", Long.toString( musicCursor.getLong(albumId) ));

                if( !MusicHelpers.isSongValidAudio(pathId) )
                    continue;

                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);

                if( !masterAlbumList.containsKey(albumID) )
                {
                    Log.d("masterAlbumList", String.format("Adding %s to HasMap.", thisAlbum));
                    Album temp = new Album(thisAlbum, thisArtist, pathId);
                    masterAlbumList.put(albumID, temp);
                }

                long thisDuration = musicCursor.getLong(durationColumn);
                masterSongList.add(new Song(thisId, pathId, thisTitle, thisArtist, albumID, thisDuration));
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
    public ArrayList<Album> getAlbums() {
        Collection<Album> albumSet = masterAlbumList.values();
        ArrayList<Album> ALAlbums = new ArrayList<>(albumSet);
        return ALAlbums;
    }
}
