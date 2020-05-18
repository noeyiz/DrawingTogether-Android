package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;

import java.util.Vector;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EraseCommand implements Command{
    private Eraser eraser = new Eraser();
    private Vector<Integer> Ids = eraser.getErasedComponentIds();

    @Override
    public void execute(Point point) {
        eraser.findComponentsToErase(point);
    }

    public Vector<Integer> getIds() {
        return Ids;
    }
}
