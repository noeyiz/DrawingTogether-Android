package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;

public class AutoDraw {
    private float width, height;
    private Point point;
    private String url;

    public AutoDraw(float width, float height, Point point, String url) {
        this.width = width;
        this.height = height;
        this.point = point;
        this.url = url;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
