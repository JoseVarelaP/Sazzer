package player.sazzer.Settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import java.util.Calendar;
import java.util.Objects;

import player.sazzer.MainActivity;
import player.sazzer.R;

public class SettingsActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_CHOOSE_PIC = 1001;

    ImageView userPic;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        sharedPreferences = getSharedPreferences(MainActivity.KEY_SHARED_PREFERENCES, MODE_PRIVATE);


        TextView userName = findViewById(R.id.name_set);

        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.settings_msg));

        userName.setText(getMessage(sharedPreferences.getString(MainActivity.KEY_NAME, null), this));

        String pic = sharedPreferences.getString(MainActivity.KEY_PICTURE, null);

        if (pic != null) {
            userPic = findViewById(R.id.image_set);
            byte[] imageByteArray = Base64.decode(pic, Base64.DEFAULT);
            RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(),
                    BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length));
            roundedBitmapDrawable.setCircular(true);
            userPic.setImageDrawable(roundedBitmapDrawable);
        }

        userPic.setOnClickListener(v -> {
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Intent intent = new Intent(Intent.ACTION_PICK, uri);
            startActivityForResult(intent, REQUEST_CODE_CHOOSE_PIC);
        });

        getSupportFragmentManager().beginTransaction().replace(R.id.containerSettings,
                new SettingsFragment()).commit();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHOOSE_PIC && resultCode == RESULT_OK) {
            Uri resultUri = data.getData();

            String temp_str = MainActivity.getStringImage(resultUri, this);

            if (temp_str != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(MainActivity.KEY_PICTURE, temp_str);
                editor.apply();

                finish();
                startActivity(getIntent());
            }
        }
    }

    public static String getMessage(String name, Activity activity) {
        Calendar cc = Calendar.getInstance();
        int mHour = cc.get(Calendar.HOUR_OF_DAY);

        String fh = null;

        if (mHour < 12) {
            fh = activity.getString(R.string.good_morning);
        }
        if (mHour > 11 && mHour < 19) {
            fh = activity.getString(R.string.good_afternoon);
        }

        if (mHour > 18) {
            fh = activity.getString(R.string.good_night);
        }

        return String.format("%s %s", fh, name);
    }
}
