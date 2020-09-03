package com.hansung.drawingtogether.view.main;

import android.view.MotionEvent;

import com.hansung.drawingtogether.data.remote.model.WarpData;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class WarpingMessage {
    private int action;
    private int pointerCount;
    private List<Integer> x = new ArrayList<>();
    private List<Integer> y = new ArrayList<>();
    private int width, height;

    public WarpingMessage(MotionEvent event, int width, int height) {
        action = event.getAction();
        pointerCount = event.getPointerCount();
        this.width = width;
        this.height = height;
        for (int i = 0; i < pointerCount; i++) {
            x.add((int)(event.getX(i)));
            y.add((int)event.getY(i));
        }
    }

    public WarpData getWarpData() {
        return new WarpData(action, x, y);
    }

}
