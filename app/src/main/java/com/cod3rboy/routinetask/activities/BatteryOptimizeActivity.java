package com.cod3rboy.routinetask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.cod3rboy.routinetask.R;

public class BatteryOptimizeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0x00000000);
        getWindow().setNavigationBarColor(0x00000000);
        setContentView(R.layout.activity_battery_optimization);
    }

    public void startMainActivity(View view) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(getString(R.string.pref_battery_optimisation), true)
                .apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
