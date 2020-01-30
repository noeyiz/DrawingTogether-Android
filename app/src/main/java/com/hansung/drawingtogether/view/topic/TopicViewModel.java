package com.hansung.drawingtogether.view.topic;

import android.view.View;

import com.hansung.drawingtogether.view.BaseViewModel;
import com.hansung.drawingtogether.R;

public class TopicViewModel extends BaseViewModel {
    public void startDrawing(View view) {
        navigate(R.id.action_topicFragment_to_drawingFragment);
    }
}
