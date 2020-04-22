package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;

import java.util.Vector;

import com.hansung.drawingtogether.data.remote.model.Log; // fixme nayeon

public class SelectCommand implements Command {
    private Selector selector = new Selector();
    private Vector<Integer> ids = new Vector<>();

    @Override
    public void execute(Point point) {
        selector.findSelectedComponent(point);
    }

    @Override
    public Vector<Integer> getIds() {
        ids.clear();
        ids.add(selector.getSelectedComponentId());
        Log.i("drawing", "ids[] = " + ids.toString());
        return ids;
    }
}