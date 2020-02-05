package com.hansung.drawingtogether.view.main;

import android.view.View;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.view.BaseViewModel;

public class MainViewModel extends BaseViewModel {
    public void clickOnline(View view) {
        navigate(R.id.action_mainFragment_to_topicFragment);
    }

    public void clickOffline(View view) {
        navigate(R.id.action_mainFragment_to_historyFragment);
    }
}
