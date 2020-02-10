package com.hansung.drawingtogether.view.main;

import android.view.View;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.view.BaseViewModel;

public class MainViewModel extends BaseViewModel {
    public void startDrawing(View view) {
        navigate(R.id.action_mainFragment_to_drawingFragment);
    }

    public void showLocalHistory(View view) {
        navigate(R.id.action_mainFragment_to_historyFragment);
    }
}
