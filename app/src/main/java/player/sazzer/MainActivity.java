package player.sazzer;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import Utilities.Utils;
import player.sazzer.Settings.SettingsActivity;
import player.sazzer.Adapters.PlaylistRecyclerViewAdapter;
import player.sazzer.DataTypes.Song;
import player.sazzer.LocalLogActivities.ActivityFirstTime;
import player.sazzer.LocalLogActivities.PrivateAudioActivity;

public class MainActivity extends AppCompatActivity implements PlaylistRecyclerViewAdapter.ItemClickListener {
    public static final int REQUEST_CODE_EXTERNAL_STORAGE = 1;

    //// here
    public static final String KEY_NAME = "USER_NAME_LOCAL_STORAGE";
    public static final String KEY_PASSWORD = "USER_PASSWORD_LOCAL_STORAGE";
    public static final String KEY_PICTURE = "USER_PIC_LOCAL_STORAGE";
    public static final String KEY_SHARED_PREFERENCES = "USER_INFO";
    public static final int REQUEST_CODE_FIRST = 167;
    public static final String KEY_LANGUAGE = "LANGUAGE_INFO";
    public static final String KEY_THEME = "THEME_INFO_STORAGE";

    SharedPreferences sharedPreferences;


    Intent playIntent = null;
    ArrayList<Song> songList = new ArrayList<>();
    private AudioServiceBinder musicSrv;

    private IntentFilter mIntentFilter;

    public static final String mBroadcasterMainActivity = "player.sazzer.action.UPDATE_SONG_VIEW";

    private final ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioServiceBinder.LocalBinder binder = (AudioServiceBinder.LocalBinder) service;
            musicSrv = binder.getService();
            //musicSrv.setCallback(MainActivity.this);
            Log.d("musicConnection","Service has started");
            sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_FETCH_SONGS) );
            sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_OBTAIN_SONGS_TO_DISPLAY) );
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    private void grantPermission(String permission, int requestCode) {
        long checkPermission = getBaseContext().checkSelfPermission(permission);

        if( checkPermission != PackageManager.PERMISSION_GRANTED ) {
            //requestPermissions(new String[]{permission},requestCode);
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        //theme(getThemePreferencesDefault());
        Utils.theme(Utils.getThemePreferencesDefault(getBaseContext()));
        setContentView(R.layout.activity_main);

        MusicHelpers.setAppContext( getApplicationContext() );

        // Hay que pedir el elemento para cargar los audios.
        // Si no, entonces tendremos un error/choque debido a la estancia de acceso ilegal de archivos.
        grantPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_EXTERNAL_STORAGE);
        //grantPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_CODE_EXTERNAL_STORAGE);

        songList = new ArrayList<>();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcasterMainActivity);

        this.registerReceiver(musicDataReciever, mIntentFilter);
        language(getBaseContext(), getLanguagePreferencesDefault());
    }

    // HERE

    private String getLanguagePreferencesDefault() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return prefs.getString(KEY_LANGUAGE, null);
    }

    private String getThemePreferencesDefault() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return prefs.getString(KEY_THEME, null);
    }

    public static void language(Context context, String language) {
        Locale location;
        if (language == null) {
            return;
        }else if (language.equals("MX")) {
            location = new Locale("es", "MX");
        } else if (language.equals("EN")){
            location = new Locale("en", "UK");
        } else {
            return;
        }

        Locale.setDefault(location);
        Configuration config = new Configuration();
        config.locale = location;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    private static void theme(String theme) {
        if (theme == null) {
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        }else if (theme.equals("LIGHT")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (theme.equals("DARK")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        }
    }

    private String getUserNameFromLocalStorage() {
        return sharedPreferences.getString(KEY_NAME, null);
    }

    private String getPasswordFromLocalStorage() {
        return sharedPreferences.getString(KEY_PASSWORD, null);
    }

    private String getPictureFromLocalStorage() {
        return sharedPreferences.getString(KEY_PICTURE, null);
    }

    private void setNameLocalStorage(String newName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_NAME, newName);
        editor.apply();
    }

    private void setPasswordLocalStorage(String newPassword) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PASSWORD, newPassword);
        editor.apply();
    }

    private void setPictureLocalStorage(String newPicture) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PICTURE, newPicture);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.audioOp) {
            Intent intent = new Intent(getBaseContext(), PrivateAudioActivity.class);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.settingsOp) {
            Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    protected void GenerateMainSongList()
    {
        Log.d("GenerateMainSongList", "Creating view for song items...");
        RecyclerView songView = findViewById(R.id.songList);
        songView.setLayoutManager(new LinearLayoutManager(this));

        Log.d("GenerateMainSongList", "The array contains " + songList.size() + " songs");

        PlaylistRecyclerViewAdapter listMusica = new PlaylistRecyclerViewAdapter(this, songList, null);
        listMusica.setClickListener(this);
        songView.setAdapter(listMusica);

        View fds = findViewById(R.id.movesection);
        fds.findViewById(R.id.button_image).setOnClickListener(v -> {
            Intent ToAlbum = new Intent( this, AllAlbumView.class );
            ToAlbum.setAction(AllAlbumView.mBroadcasterMainActivity);
            ToAlbum.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            ToAlbum.putExtra("Audio.SongArray", MusicHelpers.ConvertSongsToJSONTable(songList));

            startActivity(ToAlbum);
        });

        Log.d("GenerateMainSongList", "Done.");
    }

    @Override
    protected void onStart() {
        Log.d("onStart","Starting");
        super.onStart();

        if (playIntent == null) {
            Log.d("onCreate","Intent is null, starting service.");
            playIntent = new Intent(this, AudioServiceBinder.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(playIntent);
            } else {
                startService(playIntent);
            }
            Log.d("onCreate","Done with setup of services.");
        }
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv = null;
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            this.unregisterReceiver(musicDataReciever);
        } catch (IllegalArgumentException e)
        {
            // Don't do anything.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /// HERE
        long checkPermission = getBaseContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (checkPermission != PackageManager.PERMISSION_GRANTED)
            return;
        sharedPreferences = getSharedPreferences(KEY_SHARED_PREFERENCES, MODE_PRIVATE);

        String userName = getUserNameFromLocalStorage();
        String userPassword = getPasswordFromLocalStorage();

        if (userName == null || userPassword == null) {
            Intent intent = new Intent(getBaseContext(), ActivityFirstTime.class);
            startActivityForResult(intent, REQUEST_CODE_FIRST);
        } else {
            Log.i("NAME", userName);
            Log.i("PASSWORD", userPassword);

            String temp = getNamePreferencesDefault();

            if (temp == null) {
                Log.i("NAME", "No esta");
            } else {
                if (!temp.equals(userName) && !temp.equals("")) {
                    userName = temp;
                    setNameLocalStorage(temp);
                }
            }
            Objects.requireNonNull(getSupportActionBar()).
                    setTitle(String.format("??%s %s!",getString(R.string.hello_main),userName));
        }
    }

    private String getNamePreferencesDefault() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return prefs.getString(KEY_NAME, null);
    }

    @Override
    public void onItemClick(View view, int position) {
        Log.d("MainActivity","Set Song: " + position );

        // Broadcast a music list reset to the service.
        sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_CLEAN_QUEUE) );

        sendBroadcast( MusicHelpers.createIntentToUpdateMusicArray(songList) );

        MusicHelpers.actionServicePlaySong(getApplicationContext(), position);

        Intent nt = MusicHelpers.sendToDetailedSongInfo(MainActivity.this, songList.get(position), musicSrv);
        //nt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(nt);
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_FETCH_SONGS) );
                sendBroadcast( MusicHelpers.quickIntentFromAction(AudioServiceAction.AUDIO_SERVICE_ACTION_OBTAIN_SONGS_TO_DISPLAY) );
            }
        }
    }

    /**
     * Callback invocado despu??s de llamar a startActivityForResult
     *
     * @param requestCode c??digo de verificaci??n de la llamadas al m??todo
     * @param resultCode resultado: OK, CANCEL, etc.
     * @param data informaci??n resultante, si existe
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FIRST && resultCode != RESULT_OK) {
            Intent intent = new Intent(getBaseContext(), ActivityFirstTime.class);
            startActivityForResult(intent, REQUEST_CODE_FIRST);
        } else {
            if (data != null) {
                setNameLocalStorage(data.getStringExtra(KEY_NAME));
                setPasswordLocalStorage(data.getStringExtra(KEY_PASSWORD));

                String temp_pic = data.getStringExtra(KEY_PICTURE);

                if (temp_pic != null) {
                    String temp_image = getStringImage(Uri.parse(temp_pic), this);
                    if (temp_image != null) {
                        setPictureLocalStorage(temp_image);
                    }
                }

            }
            finish();
            startActivity(getIntent());
        }
    }

    public static String getStringImage(Uri uri, Activity activity) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);

        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    // Receieve broadcasts from other classes.
    private final BroadcastReceiver musicDataReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            Log.d("MainActivity","Recieved a broadcast");

            if( extras == null )
                return;

            if( intent.getAction().equals(mBroadcasterMainActivity) )
            {
                Log.d(mBroadcasterMainActivity, "Creating list");
                String newsongsStr = intent.getStringExtra("Audio.SongArray");

                if( newsongsStr != null && !newsongsStr.isEmpty() ) {
                    Log.d(mBroadcasterMainActivity, "List contains data! " + newsongsStr);
                    songList = MusicHelpers.ConvertJSONToTracks( newsongsStr );

                    // With song infomation created, we can now generate the song list safely.
                    GenerateMainSongList();
                }
            }
        }
    };
}
