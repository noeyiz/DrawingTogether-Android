package com.hansung.drawingtogether.view.drawing;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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
    protected String strokeColor;
    protected String fillColor;
    protected int strokeAlpha;
    protected int fillAlpha;
    protected int strokeWidth;
    protected float drawnCanvasWidth;       //component 가 그려진 canvas width (비율 계산)
    protected float drawnCanvasHeight;      //component 가 그려진 canvas height (비율 계산)
    protected float xRatio = 1;
    protected float yRatio = 1;
    protected Point beginPoint = null;
    protected Point endPoint = null;
    protected Point datumPoint = null;      //도형의 왼쪽 위 꼭짓점
    protected int width;
    protected int height;
    protected boolean isErased = false;     //픽셀 지우개에 사용될 변수
    protected boolean isSelected = false;
    protected PenMode penMode = PenMode.NORMAL;

    public void addPoint(Point point) {
        this.points.add(point);
    }

    public void clearPoints() {
        this.points.clear();
    }

    public void calculateRatio(float myCanvasWidth, float myCanvasHeight) {
        this.xRatio = myCanvasWidth / this.drawnCanvasWidth;
        this.yRatio = myCanvasHeight / this.drawnCanvasHeight;
    }

    public DrawingComponent clone() {
        if (this.type == null) { return null; }

        DrawingComponent dComponent = null;

        switch (this.type) {
            case STROKE:
                dComponent = new Stroke();
                break;
            case RECT:
                dComponent = new Rect();
                break;
            case OVAL:
                dComponent = new Oval();
                break;
        }

        dComponent.points = this.points;
        dComponent.id = this.id;
        dComponent.username = this.username;
        dComponent.usersComponentId = this.usersComponentId;
        dComponent.type = this.type;
        dComponent.strokeColor = this.strokeColor;
        dComponent.fillColor = this.fillColor;
        dComponent.strokeAlpha = this.strokeAlpha;
        dComponent.fillAlpha = this.fillAlpha;
        dComponent.strokeWidth = this.strokeWidth;
        dComponent.drawnCanvasWidth = this.drawnCanvasWidth;
        dComponent.drawnCanvasHeight = this.drawnCanvasHeight;
        dComponent.xRatio = this.xRatio;
        dComponent.yRatio = this.yRatio;
        dComponent.beginPoint = this.beginPoint;
        dComponent.endPoint = this.endPoint;
        dComponent.datumPoint = this.datumPoint;
        dComponent.width = this.width;
        dComponent.height = this.height;
        dComponent.isErased = this.isErased;
        dComponent.isSelected = this.isSelected;
        dComponent.penMode = this.penMode;

        return dComponent;
    }

    public DrawingComponent cloneAttribute() {
        if (this.type == null) { return null; }

        DrawingComponent dComponent = null;

        switch (this.type) {
            case STROKE:
                dComponent = new Stroke();
                break;
            case RECT:
                dComponent = new Rect();
                break;
            case OVAL:
                dComponent = new Oval();
                break;
        }


        dComponent.username = this.username;
        dComponent.usersComponentId = this.usersComponentId;
        dComponent.type = this.type;
        dComponent.penMode = this.penMode;
        dComponent.strokeColor = this.strokeColor;
        dComponent.fillColor = this.fillColor;
        dComponent.strokeAlpha = this.strokeAlpha;
        dComponent.fillAlpha = this.fillAlpha;
        dComponent.strokeWidth = this.strokeWidth;
        dComponent.drawnCanvasWidth = this.drawnCanvasWidth;
        dComponent.drawnCanvasHeight = this.drawnCanvasHeight;
        dComponent.calculateRatio(dComponent.drawnCanvasWidth, dComponent.drawnCanvasHeight);   //화면 비율 계산


        return dComponent;
    }

    public abstract void draw(Canvas canvas);
    public abstract void drawComponent(Canvas canvas);

}
