package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;

import java.util.Vector;

public interface Command {
    void execute(Point point);
    Vector<Integer> getIds();
}
