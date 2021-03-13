package com.cod3rboy.routinetask.fragments;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.receivers.TodayTaskWidgetProvider;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        Preference timeFormatPref = findPreference(getString(R.string.settings_time_format_key));
        if(timeFormatPref != null){
            timeFormatPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Manually save the preference
                    ListPreference listPreference = (ListPreference) preference;
                    listPreference.setValue((String) newValue);
                    // Update widgets
                    TodayTaskWidgetProvider.refreshWidgets();
                    return false;
                }
            });
        }
    }
}
