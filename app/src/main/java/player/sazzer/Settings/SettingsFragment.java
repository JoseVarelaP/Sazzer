package player.sazzer.Settings;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

import player.sazzer.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
