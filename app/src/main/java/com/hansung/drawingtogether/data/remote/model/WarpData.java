package com.hansung.drawingtogether.data.remote.model;

import android.graphics.Point;

import java.util.List;

public class WarpData {
    private int action;
    private Point[] points;

    public WarpData(int action, List<Integer> x, List<Integer> y) {
        this.action = action;
        this.points = new Point[x.size()];
        for (int i = 0; i < x.size(); i++)
            points[i] = new Point(x.get(i), y.get(i));
    }

    public int getAction() {
        return action;
    }

    public Point[] getPoints() {
        return points;
    }
}
