package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;

import java.util.Random;

import androidx.annotation.Nullable;
import lombok.Getter;

@Getter
public class DrawingView extends View {
    private DrawingEditor de = DrawingEditor.getInstance();
    private MQTTClient client = MQTTClient.getInstance();
    private JSONParser parser = JSONParser.getInstance();

    private DrawingTool dTool = new DrawingTool();
    private Command eraserCommand = new EraseCommand();
    private Command selectCommand = new SelectCommand();

    private float canvasWidth;
    private float canvasHeight;
    private DrawingComponent dComponent;
    private Stroke stroke = new Stroke();
    private Rect rect = new Rect();
    private Oval oval = new Oval();

    public DrawingView(Context context) {
        super(context);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        canvasWidth = w;
        canvasHeight = h;
        //de.setMyUsername("mm"); //fixme myUsername

        de.setDrawingBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
        de.setBackCanvas(new Canvas(de.getDrawingBitmap()));

        de.initDrawingBoardArray(w, h);
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(de.getDrawingBitmap(), 0, 0, null);

        //Log.i("drawing", "onDraw");
        //this.invalidate();
    }

    //Mode 검사 후
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //fixme switch 외에 다른 방법


        setEditorAttribute();
        switch (de.getCurrentMode()) {
            case DRAW:
                setDrawingComponentType();
                return onTouchDrawMode(event);

            case ERASE:
                return onTouchEraseMode(event);

            case SELECT:
                return onTouchSelectMode(event);

            case GROUP:
                break;
        }
        return super.onTouchEvent(event);
    }

    public void setEditorAttribute() {
        de.setCurrentType(de.getCurrentType());
        de.setUsername(de.getMyUsername());
        de.setDrawnCanvasWidth(canvasWidth);
        de.setDrawnCanvasHeight(canvasHeight);
        de.setMyCanvasWidth(canvasWidth);
        de.setMyCanvasHeight(canvasHeight);

        Random random = new Random();   //fixme
        int color = Color.rgb(random.nextInt(128) +  128, random.nextInt(128) +  128, random.nextInt(128) +  128);
        de.setStrokeColor(color);
        de.setFillColor(color);
    }

    public void setDrawingComponentType() {
        switch(de.getCurrentType()) {
            case STROKE:
                dComponent = stroke;
                break;
            case RECT:
                dComponent = rect;
                break;
            case OVAL:
                dComponent = oval;
                break;
        }
    }

    public void setComponentAttribute(DrawingComponent dComponent) {
        dComponent.setId(de.componentIdCounter());  //id 자동 증가
        dComponent.setUsername(de.getUsername());
        dComponent.setType(de.getCurrentType());
        dComponent.setFillColor(de.getFillColor());
        dComponent.setStrokeColor(de.getStrokeColor());
        dComponent.setStrokeAlpha(de.getStrokeAlpha());
        dComponent.setFillAlpha(de.getFillAlpha());
        dComponent.setStrokeWidth(de.getStrokeWidth());
        dComponent.setDrawnCanvasWidth(de.getDrawnCanvasWidth());
        dComponent.setDrawnCanvasHeight(de.getDrawnCanvasHeight());
        dComponent.calculateRatio(de.getMyCanvasWidth(), de.getMyCanvasHeight());   //화면 비율 계산

    }

    public void addPointAndDraw(DrawingComponent dComponent, Point point) {
        dComponent.setPreSize(dComponent.getPoints().size());
        dComponent.addPoint(point);

        dComponent.setBeginPoint(dComponent.getPoints().get(0));
        dComponent.setEndPoint(point);

        //de.putDrawingBitmapMapAndDraw(dComponent.getId());
        dComponent.draw(de.getBackCanvas());
    }

    public void initDrawingComponent() {
        switch(de.getCurrentType()) {
            case STROKE:
                stroke = new Stroke();
                break;
            case RECT:
                rect = new Rect();
                break;
            case OVAL:
                oval = new Oval();
                break;
        }
    }

    boolean isExit = false;
    public boolean onTouchDrawMode(MotionEvent event/*, DrawingComponent dComponent*/) {
        Point point;
        MqttMessageFormat messageFormat;

        if(isExit && event.getAction() != MotionEvent.ACTION_DOWN) {
            Log.i("mqtt", "isExit1 = " + isExit);
            return true;
        }

        if(event.getX()-5 < 0 || event.getY()-5 < 0 || de.getDrawnCanvasWidth()-5 < event.getX() || de.getDrawnCanvasHeight()-5 < event.getY()) {   //fixme 반응이 느려서 임시로 -5
            Log.i("drawing", "id=" + dComponent.getId() + ", username=" + dComponent.getUsername() + ", begin=" + dComponent.getBeginPoint() + ", end=" + dComponent.getEndPoint());

            point = dComponent.getEndPoint();
            addPointAndDraw(dComponent, point);

            //publish
            messageFormat = new MqttMessageFormat(de.getUsername(), de.getCurrentMode(), de.getCurrentType(), dComponent, MotionEvent.ACTION_UP);
            client.publish(client.getTopic_data(),  parser.jsonWrite(messageFormat));

            initDrawingComponent();

            de.splitPoints(dComponent, de.getMyCanvasWidth(), de.getMyCanvasHeight());
            de.removeCurrentComponents(dComponent.getId());
            de.addDrawingComponents(dComponent);

            de.addHistory(new DrawingItem(de.getCurrentMode(), dComponent/*, de.getDrawingBitmap()*/));
            Log.i("drawing", "history.size()=" + de.getHistory().size());
            de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));

            isExit = true;
            Log.i("mqtt", "isExit2 = " + isExit);
            return true;
        }

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isExit = false;
                Log.i("mqtt", "isExit3 = " + isExit);
                de.addCurrentComponents(dComponent);
                Log.i("drawing", "currentComponents.size() = " + de.getCurrentComponents().size());

                setComponentAttribute(dComponent);

                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point);

                //publish
                messageFormat = new MqttMessageFormat(de.getUsername(), de.getCurrentMode(), de.getCurrentType(), dComponent, event.getAction());
                client.publish(client.getTopic_data(),  parser.jsonWrite(messageFormat));

                return true;

            case MotionEvent.ACTION_MOVE:

                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point);

                //publish
                messageFormat = new MqttMessageFormat(de.getUsername(), de.getCurrentMode(), de.getCurrentType(), dComponent, event.getAction());
                client.publish(client.getTopic_data(),  parser.jsonWrite(messageFormat));

                return true;

            case MotionEvent.ACTION_UP:
                Log.i("drawing", "up");
                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point);

                Log.i("drawing", "id=" + dComponent.getId() + ", username=" + dComponent.getUsername() + ", begin=" + dComponent.getBeginPoint() + ", end=" + dComponent.getEndPoint());

                //publish
                messageFormat = new MqttMessageFormat(de.getUsername(), de.getCurrentMode(), de.getCurrentType(), dComponent, event.getAction());
                client.publish(client.getTopic_data(),  parser.jsonWrite(messageFormat));
                initDrawingComponent();

                de.splitPoints(dComponent, de.getMyCanvasWidth(), de.getMyCanvasHeight());

                de.removeCurrentComponents(dComponent.getId());
                de.addDrawingComponents(dComponent);
                Log.i("drawing", "drawingComponents.size() = " + de.getDrawingComponents().size());

                de.addHistory(new DrawingItem(de.getCurrentMode(), dComponent/*, de.getDrawingBitmap()*/));
                Log.i("drawing", "history.size()=" + de.getHistory().size());
                de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));

                de.clearUndoArray();

                return true;
        }
        return true;
    }

    public boolean onTouchEraseMode(MotionEvent event) {

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                dTool.setCommand(eraserCommand);

                Point point = new Point((int)event.getX(), (int)event.getY());
                dTool.doCommand(point);

                return true;
        }
        return true;
    }

    Point selectDownPoint;
    boolean isSelected = false;
    public boolean onTouchSelectMode(MotionEvent event) {

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                selectDownPoint = new Point((int)event.getX(), (int)event.getY());
                Log.i("drawing", "select down = " + selectDownPoint);

                return true;

            case MotionEvent.ACTION_UP:

                if(selectDownPoint.equals(new Point((int)event.getX(), (int)event.getY()))) {

                    isSelected = true;
                    Log.i("drawing", "isSelected = " + isSelected);

                    return true;
                } else if(isSelected) {
                    //dTool.setCommand(selectCommand);
                    //dTool.doCommand(selectDownPoint);

                    Log.i("drawing", "do select command");
                }
        }
        return true;
    }

    public void clear() {
        de.clear();
    }

    public void undo() {
        /*de.undo();
        invalidate();
        Log.i("drawing", "history.size()=" + de.getHistory().size());*/
    }

    public void redo() {
        /*de.redo();
        invalidate();
        Log.i("drawing", "history.size()=" + de.getHistory().size());*/
    }

}