package com.cod3rboy.routinetask.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.cod3rboy.routinetask.R;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        MaterialToolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> {
            finishAfterTransition();
        });
    }


}