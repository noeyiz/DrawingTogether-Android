package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;

public class SelectCommand implements Command {
    private Selector selector = new Selector();

    @Override
    public void execute(Point point) {
        selector.select(point);
    }
}