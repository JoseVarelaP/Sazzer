package player.sazzer.LocalLogActivities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import player.sazzer.MainActivity;
import player.sazzer.R;

public class ActivityPassword extends AppCompatActivity {

    EditText password;
    Button button;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_private_activity);

        Objects.requireNonNull(getSupportActionBar()).hide();

        password = findViewById(R.id.user_password2);
        button = findViewById(R.id.btn_next2);

        button.setOnClickListener(v -> {
            if (!verifyPassword()) {
                Toast.makeText(this, "Wrong Password", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        });
    }

    private boolean verifyPassword() {
        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.KEY_SHARED_PREFERENCES, MODE_PRIVATE);
        String passwordStr = sharedPreferences.getString(MainActivity.KEY_PASSWORD, null);

        if (passwordStr == null) {
            return false;
        }

        return passwordStr.equals(password.getText().toString());
    }
}
