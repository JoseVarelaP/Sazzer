package player.sazzer.LocalLogActivities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import player.sazzer.MainActivity;
import player.sazzer.R;

public class ActivityFirstTime extends Activity {

    EditText userName, passwordUser;
    Button btnNext, btnChoosePicture;
    ImageView profilePic;
    String stringImage;
    private static final int REQUEST_CODE_CHOOSE_PIC = 10;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.first_time_activity);

        userName = findViewById(R.id.user_name);
        passwordUser = findViewById(R.id.user_password);

        btnNext = findViewById(R.id.btn_next);
        btnChoosePicture = findViewById(R.id.btn_choose_pic);

        profilePic = findViewById(R.id.profile_pic);

        btnNext.setOnClickListener(v -> {
            if (!verifyData())
                return;
            Intent returnIntent = new Intent();
            returnIntent.putExtra(MainActivity.KEY_NAME, userName.getText().toString());
            returnIntent.putExtra(MainActivity.KEY_PASSWORD, passwordUser.getText().toString());
            returnIntent.putExtra(MainActivity.KEY_PICTURE, stringImage);
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
        });

        btnChoosePicture.setOnClickListener(v -> {
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Intent intent = new Intent(Intent.ACTION_PICK, uri);
            startActivityForResult(intent, REQUEST_CODE_CHOOSE_PIC);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHOOSE_PIC && resultCode == RESULT_OK) {
            Uri resultUri = data.getData();
            //profilePic.setImageURI(resultUri);
            stringImage = getStringImage(resultUri);

            if (stringImage != null) {
                Log.i("PIC_TEST","G0");
                byte[] imageByteArray = Base64.decode(stringImage, Base64.DEFAULT);
                profilePic.setImageBitmap(BitmapFactory.decodeByteArray(imageByteArray,
                        0, imageByteArray.length));
            }

            Log.i("PIC_TEST","YEAH");
        }
    }

    private String getStringImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);

        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private boolean verifyData() {
        return !userName.getText().toString().equals("") && !passwordUser.getText().toString().equals("");
    }
}
