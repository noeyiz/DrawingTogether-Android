package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EraseCommand implements Command{
    private Eraser eraser = new Eraser();

    @Override
    public void execute(Point point) {
        eraser.findComponentsToErase(point);
    }
}
