package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;

import java.util.Random;

import androidx.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrawingView extends View {
    private DrawingEditor de = DrawingEditor.getInstance();
    private MQTTClient client = MQTTClient.getInstance();
    private JSONParser parser = JSONParser.getInstance();

    private DrawingTool dTool = new DrawingTool();
    private Command eraserCommand = new EraseCommand();
    private Command selectCommand = new SelectCommand();

    private float canvasWidth;
    private float canvasHeight;
    private int currentDrawAction = MotionEvent.ACTION_UP;
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
        Log.e("DrawingView", "call onSizeChanged");

        canvasWidth = w;
        canvasHeight = h;

        if(de.getDrawingBitmap() == null) {
            de.setDrawingBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            de.setLastDrawingBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            de.setBackCanvas(new Canvas(de.getDrawingBitmap()));
        }
        if(de.getDrawingBoardArray() == null) {
            de.initDrawingBoardArray(w, h);
        }
        //if(client.isMaster()) {
            Log.i("mqtt", "progressDialog dismiss");
            client.getProgressDialog().dismiss();
        //}
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(de.getDrawingBitmap(), 0, 0, null);
        //Log.i("drawing", "onDraw");
        //this.invalidate();
    }

    boolean isIntercept = false;
    //Mode 검사 후
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isIntercept) {
            this.getParent().requestDisallowInterceptTouchEvent(true);
        } else {
            Log.i("drawing", "intercept drawing view touch");
            return false;
        }
        //this.getParent().requestDisallowInterceptTouchEvent(true);

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

        /*
        Random random = new Random();   //fixme nayeon
        int color = Color.rgb(random.nextInt(128) +  128, random.nextInt(128) +  128, random.nextInt(128) +  128);
        de.setStrokeColor(color);
        de.setFillColor(color);
        */
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
        dComponent.setUsersComponentId(dComponent.username + "-" + dComponent.id);
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
        dComponent.setPreSize(dComponent.getPoints().size()); // 컴포넌트를 구성하는 포인트 배열의 사이즈 저장
        dComponent.addPoint(point); // 포인트 추가
        dComponent.setBeginPoint(dComponent.getPoints().get(0)); // 시작 포인트 설정
        dComponent.setEndPoint(point); // 끝 포인트 설정

        dComponent.draw(de.getBackCanvas());

        if(dComponent.getType() == ComponentType.STROKE) {
            Canvas canvas = new Canvas(de.getLastDrawingBitmap()); // 새로운 캔버스를 생성해 그려주기
            dComponent.draw(canvas);
        }
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

    public void sendModeMqttMessage() {
        MqttMessageFormat messageFormat = new MqttMessageFormat(de.getMyUsername(), de.getCurrentMode());
        client.publish(client.getTopic_data(), parser.jsonWrite(messageFormat));
    }

    public void sendDrawMqttMessage(int action) {
        MqttMessageFormat messageFormat = new MqttMessageFormat(de.getMyUsername(), de.getCurrentMode(), de.getCurrentType(), dComponent, action);
        client.publish(client.getTopic_data(),  parser.jsonWrite(messageFormat));
    }

    public void updateDrawingComponentId(DrawingComponent dComponent) {
        try {
            DrawingComponent upComponent = de.findCurrentComponent(dComponent.getUsersComponentId());
            Log.i("drawing", "upComponent: id=" + upComponent.getId() + ", endPoint=" + upComponent.getEndPoint().toString());
            dComponent.setId(upComponent.getId());
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void doInDrawActionUp(DrawingComponent dComponent) {
        initDrawingComponent(); // 드로잉 컴포넌트 객체 생성

        de.splitPoints(dComponent, de.getMyCanvasWidth(), de.getMyCanvasHeight());
        de.removeCurrentComponents(dComponent.getId());
        de.addDrawingComponents(dComponent);
        Log.i("drawing", "drawingComponents.size() = " + de.getDrawingComponents().size());

        de.addHistory(new DrawingItem(de.getCurrentMode(), dComponent/*, de.getDrawingBitmap()*/)); // 드로잉 컴포넌트가 생성되면 History 에 저장

        if(de.getHistory().size() == 1)
            de.getDrawingFragment().getBinding().undoBtn.setEnabled(true);

        Log.i("drawing", "history.size()=" + de.getHistory().size());
        if(dComponent.getType() != ComponentType.STROKE) // 도형이 그려졌다면 lastDrawingBitmap 에 drawingBitmap 내용 복사
            de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
        de.clearUndoArray();
    }

    public void InterceptTouchEventAndDoActionUp() {
        if(currentDrawAction == MotionEvent.ACTION_UP) {
            return;
        }
        isIntercept = true;
        Log.i("drawing", "intercept touch event and do action up");

        this.getParent().requestDisallowInterceptTouchEvent(false);
        addPointAndDraw(dComponent, dComponent.getEndPoint());
        sendDrawMqttMessage(MotionEvent.ACTION_UP);
        updateDrawingComponentId(dComponent);
        doInDrawActionUp(dComponent);
    }

    boolean isExit = false;
    public boolean onTouchDrawMode(MotionEvent event/*, DrawingComponent dComponent*/) {
        Point point;
        currentDrawAction = event.getAction();

        if(isExit && event.getAction() != MotionEvent.ACTION_DOWN) {
            Log.i("mqtt", "isExit1 = " + isExit);
            return true;
        }

        // 터치가 DrawingView 밖으로 나갔을 때
        if(event.getX()-5 < 0 || event.getY()-5 < 0 || de.getDrawnCanvasWidth()-5 < event.getX() || de.getDrawnCanvasHeight()-5 < event.getY()) {   //fixme 반응이 느려서 임시로 -5
            currentDrawAction = MotionEvent.ACTION_UP;
            Log.i("drawing", "id=" + dComponent.getId() + ", username=" + dComponent.getUsername() + ", begin=" + dComponent.getBeginPoint() + ", end=" + dComponent.getEndPoint());
            Log.i("drawing", "exit");

            point = dComponent.getEndPoint();
            addPointAndDraw(dComponent, point);

            //publish
            sendDrawMqttMessage(MotionEvent.ACTION_UP);
            updateDrawingComponentId(dComponent);
            doInDrawActionUp(dComponent);

            isExit = true;
            Log.i("mqtt", "isExit2 = " + isExit);
            return true;
        }

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isExit = false;
                Log.i("mqtt", "isExit3 = " + isExit);

                /*de.addCurrentComponents(dComponent);
                Log.i("drawing", "currentComponents.size() = " + de.getCurrentComponents().size());
*/
                setComponentAttribute(dComponent);

                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point);

                //publish
                sendDrawMqttMessage(event.getAction());
                return true;

            case MotionEvent.ACTION_MOVE:
                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point);

                //publish
                sendDrawMqttMessage(event.getAction());
                return true;

            case MotionEvent.ACTION_UP:
                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point);
                Log.i("drawing", "id=" + dComponent.getId() + ", username=" + dComponent.getUsername() + ", begin=" + dComponent.getBeginPoint() + ", end=" + dComponent.getEndPoint());

                //publish
                sendDrawMqttMessage(event.getAction());
                updateDrawingComponentId(dComponent);
                doInDrawActionUp(dComponent);
                return true;
            default:
                Log.i("drawing", "action = " + MotionEvent.actionToString(event.getAction()));
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
        AlertDialog.Builder builder = new AlertDialog.Builder(de.getDrawingFragment().getActivity());
        builder.setTitle("화면 초기화").setMessage("모든 그리기 내용이 삭제됩니다.\n그래도 지우시겠습니까?");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                de.setCurrentMode(Mode.CLEAR);
                sendModeMqttMessage();
                de.clearDrawingComponents();
                de.clearTexts();
                de.getDrawingFragment().getBinding().redoBtn.setEnabled(false);
                de.getDrawingFragment().getBinding().undoBtn.setEnabled(false);
                invalidate();

                Log.i("drawing", "history.size()=" + de.getHistory().size());
                Log.i("drawing", "clear");
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i("drawing", "canceled");
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void clearBackgroundImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(de.getDrawingFragment().getActivity());
        builder.setTitle("배경 초기화").setMessage("배경 이미지가 삭제됩니다.\n그래도 지우시겠습니까?");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                de.setCurrentMode(Mode.CLEAR_BACKGROUND_IMAGE);
                sendModeMqttMessage();
                de.setBackgroundImage(null);
                de.clearBackgroundImage();

                Log.i("drawing", "clear background image");
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i("drawing", "canceled");
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void undo() {
        Mode preMode = de.getCurrentMode();
        de.setCurrentMode(Mode.UNDO);
        sendModeMqttMessage();
        de.undo();

        if(de.getUndoArray().size() == 1)
            de.getDrawingFragment().getBinding().redoBtn.setEnabled(true);

        if(de.getHistory().size() == 0)
            de.getDrawingFragment().getBinding().undoBtn.setEnabled(false);

        invalidate();
        de.setCurrentMode(preMode);
    }

    public void redo() {
        Mode preMode = de.getCurrentMode();
        de.setCurrentMode(Mode.REDO);
        sendModeMqttMessage();
        de.redo();
        if(de.getHistory().size() == 1)
            de.getDrawingFragment().getBinding().undoBtn.setEnabled(true);

        if(de.getUndoArray().size() == 0)
            de.getDrawingFragment().getBinding().redoBtn.setEnabled(false);

        invalidate();
        de.setCurrentMode(preMode);
    }
}
