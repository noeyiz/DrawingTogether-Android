package com.hansung.drawingtogether.view.drawing;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hansung.drawingtogether.data.remote.model.MyLog;


public class DrawingViewController extends FrameLayout {


    public DrawingViewController(@NonNull Context context) {
        super(context);
    }

    public DrawingViewController(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawingViewController(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(ev.getAction() == MotionEvent.ACTION_CANCEL) {
            MyLog.i("drawing", "intercept complete");
            return true;
        }

        return super.onInterceptTouchEvent(ev);
    }
}

