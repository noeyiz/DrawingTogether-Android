package com.hansung.drawingtogether.view.drawing;

import android.graphics.Canvas;
import android.graphics.Point;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class DrawingComponent {
    protected ArrayList<Point> points = new ArrayList<>();
    protected int id;
    protected String username = null;
    protected String usersComponentId = null;
    protected ComponentType type = null;
    protected int strokeColor;
    protected int fillColor;            //도형에서 사용
    protected int strokeAlpha;
    protected int fillAlpha;
    protected int strokeWidth;
    protected int preSize;              //addPoint() 전에 points 의 이전 크기를 저장
    protected float drawnCanvasWidth;   //draw 되는 canvas width
    protected float drawnCanvasHeight;  //draw 되는 canvas height
    protected float xRatio = 1;
    protected float yRatio = 1;
    protected Point beginPoint = null;
    protected Point endPoint = null;
    protected Point datumPoint = null;  //사각형의 왼쪽 위 꼭짓점
    protected int width;
    protected int height;
    protected Boolean isErased = false;
    //protected byte[] byteArray;
    //protected Canvas myCanvas;
    //protected Bitmap bitmap;

    public void addPoint(Point point) {
        this.points.add(point);
    }

    public void clearPoints() {
        this.points.clear();
    }

    public int getPointsSize() { return points.size(); }

    public void calculateRatio(float myCanvasWidth, float myCanvasHeight) {
        this.xRatio = myCanvasWidth / this.drawnCanvasWidth;
        this.yRatio = myCanvasHeight / this.drawnCanvasHeight;
    }

    public abstract void draw(Canvas canvas);
    public abstract void drawComponent(Canvas canvas);

    public abstract String toString();

}
