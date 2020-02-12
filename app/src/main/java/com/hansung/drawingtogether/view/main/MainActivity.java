package com.hansung.drawingtogether.view.main;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.hansung.drawingtogether.R;

public class MainActivity extends AppCompatActivity {

    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBar = getSupportActionBar();
    }

    public void setTitleBar(String title) {
        if (actionBar != null)
            actionBar.setTitle(title);
    }
}
