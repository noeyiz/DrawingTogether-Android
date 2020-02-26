package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;

public interface Command {
    void execute(Point point);
}
