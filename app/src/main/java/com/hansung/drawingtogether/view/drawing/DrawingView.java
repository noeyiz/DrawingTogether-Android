package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrawingView extends View {
    private DrawingEditor de = DrawingEditor.getInstance();
    private MQTTClient client = MQTTClient.getInstance();
    private final JSONParser parser = JSONParser.getInstance();
    private SendMqttMessage sendMqttMessage = SendMqttMessage.getInstance();
    private int msgChunkSize = 20;
    private ArrayList<Point> points = new ArrayList<>(msgChunkSize);
    //private Point[] points;

    private String topicData;

    private DrawingTool dTool = new DrawingTool();
    private Command eraserCommand = new EraseCommand();
    private Command selectCommand = new SelectCommand();

    private boolean isIntercept = false;
    //private int currentDrawAction = MotionEvent.ACTION_UP;
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
        MyLog.e("DrawingView", "call onSizeChanged");

        topicData = client.getTopic_data();
        canvasWidth = w;
        canvasHeight = h;

        if(de.getDrawingBitmap() == null) {
            de.setDrawingBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            de.setLastDrawingBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));

            de.setPreSelectedComponentsBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            de.setPostSelectedComponentsBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            de.setSelectedComponentBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));

            de.setBackCanvas(new Canvas(de.getDrawingBitmap()));
        }
        if(de.getDrawingBoardArray() == null) {
            de.initDrawingBoardArray(w, h);
        }
        if(client.isMaster()) {
            MyLog.i("mqtt", "progressDialog dismiss");
            client.getProgressDialog().dismiss();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        try {   //중간자 들어오다가 브로커 연결 유실되면 NullPointerException 발생
            canvas.drawBitmap(de.getDrawingBitmap(), 0, 0, null);
            canvas.drawBitmap(de.getSelectedComponentBitmap(), 0, 0, null);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        //Log.i("drawing", "onDraw");
        //this.invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //MyLog.i("intercept", "drawing view isIntercept = " + isIntercept + ", de isIntercept = " + de.isIntercept());

        if(!de.isIntercept()) {
            this.isIntercept = false;
            //MyLog.i("intercept", "DrawingView false");
        }
        if(this.isIntercept || ((event.getAction() == MotionEvent.ACTION_DOWN) && de.isIntercept())) {
            MyLog.i("drawing", "intercept drawing view touch");
            return false;
        } else {
            this.getParent().requestDisallowInterceptTouchEvent(true);
        }

        setEditorAttribute();

        if(de.getCurrentMode() != Mode.SELECT) {
            de.initSelectedBitmap();
        }

        switch (de.getCurrentMode()) {
            case DRAW:
                if(event.getAction() == MotionEvent.ACTION_DOWN)
                    initDrawingComponent(); // 드로잉 컴포넌트 객체 생성
                setDrawingComponentType();
                return onTouchDrawMode(event);

            case ERASE:
                return onTouchEraseMode(event);

            case SELECT:
                return onTouchSelectMode(event);

            case GROUP:
                break;
            case WARP:
                return onTouchWarpMode(event);
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
                de.setDrawingShape(true);
                break;
            case OVAL:
                dComponent = oval;
                de.setDrawingShape(true);
                break;
        }
    }

    public void setComponentAttribute(DrawingComponent dComponent) {
        //dComponent.setId(de.componentIdCounter());  //id 자동 증가
        dComponent.setUsername(de.getUsername());
        dComponent.setUsersComponentId(de.usersComponentIdCounter());
        dComponent.setType(de.getCurrentType());
        dComponent.setFillColor(de.getFillColor());
        dComponent.setStrokeColor(de.getStrokeColor());
        dComponent.setStrokeAlpha(de.getStrokeAlpha());
        dComponent.setFillAlpha(de.getFillAlpha());
        dComponent.setStrokeWidth(de.getStrokeWidth());
        dComponent.setDrawnCanvasWidth(de.getDrawnCanvasWidth());
        dComponent.setDrawnCanvasHeight(de.getDrawnCanvasHeight());
        dComponent.calculateRatio(de.getMyCanvasWidth(), de.getMyCanvasHeight());   //화면 비율 계산
        //dComponent.setSelected(false);
    }

    public void addPointAndDraw(DrawingComponent dComponent, Point point) {
        dComponent.setPreSize(dComponent.getPoints().size());
        dComponent.addPoint(point);
        dComponent.setBeginPoint(dComponent.getPoints().get(0));
        dComponent.setEndPoint(point);

        dComponent.draw(de.getBackCanvas());

        if(dComponent.getType() == ComponentType.STROKE) {
            Canvas canvas = new Canvas(de.getLastDrawingBitmap());
            dComponent.draw(canvas);
        } /*else {
            de.updateCurrentShapes(dComponent);
        }*/
    }

    public void addPoint(DrawingComponent dComponent, Point point) {
        dComponent.setPreSize(dComponent.getPoints().size());
        dComponent.addPoint(point);
        dComponent.setBeginPoint(dComponent.getPoints().get(0));
        dComponent.setEndPoint(point);
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

    /*public void sendDrawMqttMessage(int action, Point point) {
        switch(action) {
            case MotionEvent.ACTION_DOWN:
                sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), dComponent, action));
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getUpdatedDrawingComponentId(dComponent), dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), point, action));
                break;
        }
    }*/

    public void sendModeMqttMessage(Mode mode) {
        MqttMessageFormat messageFormat = new MqttMessageFormat(de.getMyUsername(), mode);
        //client.publish(topicData, parser.jsonWrite(messageFormat));
        sendMqttMessage.putMqttMessage(messageFormat);  //todo minj
    }

    public void sendSelectMqttMessage(boolean isSelected) {
        sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), Mode.SELECT, isSelected));

    }

    public void redrawShape(DrawingComponent dComponent) {
        if(dComponent.getType() != ComponentType.STROKE) { // 도형이 그려졌다면 lastDrawingBitmap 에 drawingBitmap 내용 복사
            if(de.getCurrentShapes().size() == 0)
                de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
            else {
                Canvas canvas = new Canvas(de.getLastDrawingBitmap());
                dComponent.draw(canvas);
            }
        }
    }

    public void doInDrawActionUp(DrawingComponent dComponent, float canvasWidth, float canvasHeight) {

        //de.removeCurrentShapes(dComponent.getUsersComponentId());
        de.splitPoints(dComponent, canvasWidth, canvasHeight);
        de.addDrawingComponents(dComponent);
        de.addHistory(new DrawingItem(de.getCurrentMode(), dComponent/*, de.getDrawingBitmap()*/)); // 드로잉 컴포넌트가 생성되면 History 에 저장
        MyLog.i("drawing", "history.size()=" + de.getHistory().size() + ", id=" + dComponent.getId());

        de.removeCurrentComponents(dComponent.getUsersComponentId());   //fixme

        if(de.getHistory().size() == 1)
            de.getDrawingFragment().getBinding().undoBtn.setEnabled(true);

        de.clearUndoArray();

        //if(de.isIntercept()) this.isIntercept = true;   //**

        //de.setDrawingShape(false);

        de.printCurrentComponents("up");
        de.printDrawingComponents("up");
    }

    /*public void InterceptTouchEventAndDoActionUp() {
        if(currentDrawAction == MotionEvent.ACTION_UP) {
            return;
        }
        isIntercept = true;
        Log.i("drawing", "intercept touch event and do action up");

        this.getParent().requestDisallowInterceptTouchEvent(false);
        sendDrawMqttMessage(MotionEvent.ACTION_UP);

        addPointAndDraw(dComponent, dComponent.getEndPoint());
        //de.updateDrawingComponentId(dComponent);
        doInDrawActionUp(dComponent);
    }*/

    boolean isExit = false;
    public boolean onTouchDrawMode(MotionEvent event/*, DrawingComponent dComponent*/) {
        Point point;
        //currentDrawAction = event.getAction();
        //de.setCurrentDrawAction(event.getAction());

        if(isExit && event.getAction() != MotionEvent.ACTION_DOWN) {
            MyLog.i("mqtt", "isExit1 = " + isExit);
            return true;
        }

        // 터치가 DrawingView 밖으로 나갔을 때
        if(event.getX()-5 < 0 || event.getY()-5 < 0 || de.getDrawnCanvasWidth()-5 < event.getX() || de.getDrawnCanvasHeight()-5 < event.getY()) {   //fixme 반응이 느려서 임시로 -5
            //currentDrawAction = MotionEvent.ACTION_UP;
            //de.setCurrentDrawAction(MotionEvent.ACTION_UP);

            //MyLog.i("drawing", "id=" + dComponent.getId() + ", username=" + dComponent.getUsername() + ", begin=" + dComponent.getBeginPoint() + ", end=" + dComponent.getEndPoint());
            MyLog.i("drawing", "exit");

            point = dComponent.getEndPoint();
            addPointAndDraw(dComponent, point);

            //doInDrawActionUp(dComponent);
            //initDrawingComponent();
            redrawShape(dComponent);

            //publish
            if(points.size() != 0) {
                MyLog.i("sendThread", "send move chunk " + points.size() + ", " + points.toString());
                sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), /*de.getUpdatedDrawingComponentId(dComponent), */dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), (ArrayList<Point>)points.clone()/*points.toString()*/, MotionEvent.ACTION_MOVE));
                points.clear();
            }
            sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), /*de.getUpdatedDrawingComponentId(dComponent), */dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), point, MotionEvent.ACTION_UP));
            //sendDrawMqttMessage(event.getAction(), point);

            isExit = true;
            MyLog.i("mqtt", "isExit2 = " + isExit);
            return true;
        }

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isExit = false;
                MyLog.i("mqtt", "isExit3 = " + isExit);

                /*de.addCurrentComponents(dComponent);
                Log.i("drawing", "currentComponents.size() = " + de.getCurrentComponents().size());
                */
                //de.addCurrentShapes(dComponent);

                setComponentAttribute(dComponent);
                point = new Point((int)event.getX(), (int)event.getY());
                //addPointAndDraw(dComponent, point);

                //publish
                //down에서는 DrawingComponent 자체를 보내고, move, up에서는 추가된 점에 관한 정보만 보낸다. fixme minj
                sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), dComponent, event.getAction()));
                //sendDrawMqttMessage(event.getAction(), point);

                return true;

            case MotionEvent.ACTION_MOVE:
                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point);

                //publish
                points.add(point);
                //MyLog.i("sendThread", points.size() + "");

                if(points.size() == msgChunkSize) {
                    MyLog.i("sendThread", "send move chunk " + points.size() + ", " + points.toString());
                    sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), /*de.getUpdatedDrawingComponentId(dComponent), */dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), (ArrayList<Point>) points.clone()/*points.toString()*/, event.getAction()));
                    points.clear();
                }
                //sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getUpdatedDrawingComponentId(dComponent), dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), point, event.getAction()));
                //sendDrawMqttMessage(event.getAction(), point);

                return true;

            case MotionEvent.ACTION_UP:
                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point);

                //MyLog.i("drawing", "id=" + dComponent.getId() + ", username=" + dComponent.getUsername() + ", begin=" + dComponent.getBeginPoint() + ", end=" + dComponent.getEndPoint());
                //doInDrawActionUp(dComponent);
                //initDrawingComponent();
                redrawShape(dComponent);

                //publish
                if(points.size() != 0) {
                    MyLog.i("sendThread", "send move chunk " + points.size() + ", " + points.toString());
                    sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), /*de.getUpdatedDrawingComponentId(dComponent), */dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), (ArrayList<Point>) points.clone()/*points.toString()*/, MotionEvent.ACTION_MOVE));
                    points.clear();
                }
                sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), /*de.getUpdatedDrawingComponentId(dComponent), */dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), point, event.getAction()));
                //sendDrawMqttMessage(event.getAction(), point);

                return true;
            default:
                MyLog.i("drawing", "action = " + MotionEvent.actionToString(event.getAction()));
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

    Point selectPrePoint;
    Point selectPostPoint;
    boolean isSelected = false;
    int selectMoveCount = 0;
    Point selectDownPoint;
    public boolean onTouchSelectMode(MotionEvent event) {
        dTool.setCommand(selectCommand);

        if(!isSelected) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    selectPrePoint = new Point((int) event.getX(), (int) event.getY());
                case MotionEvent.ACTION_MOVE:
                    selectPostPoint = new Point((int) event.getX(), (int) event.getY());
                    if (!selectPrePoint.equals(selectPostPoint)) {
                        selectMoveCount++;
                        MyLog.i("drawing", "move pre=" + selectPrePoint.toString() + ", post=" + selectPostPoint.toString() + ", " + selectMoveCount);

                        selectPrePoint = selectPostPoint;
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    dTool.doCommand(selectPostPoint);
                    int selectedComponentId = dTool.getIds().get(0);
                    if (selectedComponentId != -1 && selectMoveCount <= 7) {    // 제스처로 할 지 고민
                        if(!Objects.requireNonNull(de.findDrawingComponentById(selectedComponentId)).isSelected()) {
                            isSelected = true;

                            de.setSelectedComponent(de.findDrawingComponentById(selectedComponentId));
                            if(de.getSelectedComponent() != null) {
                                if(de.getSelectedComponent().isSelected()) {
                                    de.getSelectedComponent().setSelected(false);
                                    //todo publish - 다른 사람들 셀렉트 가능 --> 모드 바뀔 때 추가로 메시지 전송 필요
                                    sendSelectMqttMessage(false);
                                    //sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), de.getCurrentMode(), false));
                                }
                            }
                            de.getSelectedComponent().setSelected(true);
                            de.setPreSelectedComponents(selectedComponentId);
                            de.setPostSelectedComponents(selectedComponentId);

                            de.clearSelectedBitmap();
                            de.drawSelectedComponentBorder(de.getSelectedComponent(), de.getMySelectedBorderColor());
                            invalidate();

                            //select success
                            MyLog.i("drawing", "selected id=" + selectedComponentId);

                            //todo publish - 다른 사람들 셀렉트 못하게
                            sendSelectMqttMessage(true);
                            //sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), de.getCurrentMode(), true));

                        } else {
                            MyLog.i("drawing", "already selected");
                            showToastMsg("다른 사람이 선택한 도형입니다");
                        }
                    } else {
                        de.initSelectedBitmap();
                        MyLog.i("drawing", "not selected=" + selectedComponentId);

                        //select cancel
                        if(de.getSelectedComponent() != null) {
                            de.getSelectedComponent().setSelected(false);
                            //todo publish - 다른 사람들 셀렉트 가능 --> 모드 바뀔 때 추가로 메시지 전송 필요
                            sendSelectMqttMessage(false);
                            //sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), de.getCurrentMode(), false));
                        }
                    }
                    selectMoveCount = 0;
                    return true;
            }
            return true;
        } else {
            int moveX, moveY;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    MyLog.i("drawing", "selected down");
                    //de.getSelectedComponent().calculateRatio(de.getMyCanvasWidth(), de.getMyCanvasHeight());
                    MyLog.i("drawing", "xRatio=" + de.getSelectedComponent().getXRatio() + ", yRatio=" + de.getSelectedComponent().getYRatio());
                    selectDownPoint = new Point((int)event.getX(), (int)event.getY());
                    if(!de.isContainsSelectedComponent(selectDownPoint)) {
                        //isSelected = false;
                        de.initSelectedBitmap();

                        de.getSelectedComponent().setSelected(false);
                        MyLog.i("drawing", "selected false");

                        //todo publish - 다른 사람들 셀렉트 가능
                        sendSelectMqttMessage(false);
                        //sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), de.getCurrentMode(), false));

                        return true;
                    }

                    de.drawAllPreSelectedComponents();
                    de.drawAllPostSelectedComponents();
                    de.drawSelectedComponent();
                    de.drawSelectedBitmaps();
                    de.drawSelectedComponentBorder(de.getSelectedComponent(), de.getMySelectedBorderColor());
                    invalidate();

                    MyLog.i("drawing", "selected true");

                    //todo publish - selected down
                    sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), de.getCurrentMode(), event.getAction(), 0, 0));

                    return true;

                case MotionEvent.ACTION_MOVE:
                    MyLog.i("drawing", "selected move");
                    //moveX = (int)(((int)event.getX() - selectDownPoint.x)*de.getSelectedComponent().getXRatio());
                    //moveY = (int)(((int)event.getY() - selectDownPoint.y)*de.getSelectedComponent().getYRatio());
                    moveX = (((int)event.getX() - selectDownPoint.x));
                    moveY = (((int)event.getY() - selectDownPoint.y));

                    //Point datumPoint = de.getSelectedComponent().getDatumPoint();
                    Point datumPoint = new Point((int)(de.getSelectedComponent().getDatumPoint().x*de.getSelectedComponent().getXRatio()), (int)(de.getSelectedComponent().getDatumPoint().y*de.getSelectedComponent().getYRatio()));

                    int width = de.getSelectedComponent().getWidth();
                    int height = de.getSelectedComponent().getHeight();

                    if((datumPoint.x+moveX-10 < 0 && moveX < 0) || (datumPoint.y+moveY-10 < 0 && moveY < 0) || (datumPoint.y+moveY+height+10 > de.getDrawnCanvasHeight() && moveY > 0) || (datumPoint.x+moveX+width+10 > de.getDrawnCanvasWidth() && moveX > 0)) {
                        return true;
                    }

                    selectDownPoint = new Point((int)event.getX(), (int)event.getY());

                    de.moveSelectedComponent(de.getSelectedComponent(), moveX, moveY);
                    de.clearSelectedBitmap();
                    de.getSelectedComponent().drawComponent(de.getSelectedCanvas());
                    de.drawSelectedComponentBorder(de.getSelectedComponent(), de.getMySelectedBorderColor());
                    invalidate();

                    //todo publish - selected move
                    sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), de.getCurrentMode(), event.getAction(), moveX, moveY));

                    return true;

                case MotionEvent.ACTION_UP:
                    MyLog.i("drawing", "selected up");

                    de.updateDrawingBitmap();
                    de.updateSelectedComponent(de.getSelectedComponent(), de.getMyCanvasWidth(), de.getMyCanvasHeight());
                    de.updateDrawingComponents(de.getSelectedComponent());
                    MyLog.i("drawing", "drawingComponents.size() = " + de.getDrawingComponents().size());

                    //de.addHistory(new DrawingItem(de.getCurrentMode(), de.getSelectedComponent())); //todo
                    //Log.i("drawing", "history.size()=" + de.getHistory().size() + ", id=" + de.getSelectedComponent().getId());

                    de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
                    de.clearUndoArray();
                    invalidate();

                    //if(de.isIntercept()) this.isIntercept = true;   //todo - DRAW mode 외에도 중간자 join 시 intercept 처리

                    //todo publish - selected up
                    sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), de.getCurrentMode(), event.getAction(), 0, 0));

                    return true;
            }
            return true;
        }
    }

    public boolean onTouchWarpMode(MotionEvent event) {
        return false;
    }

    public void clear() {
        AlertDialog.Builder builder = new AlertDialog.Builder(de.getDrawingFragment().getActivity());
        builder.setTitle("화면 초기화").setMessage("모든 그리기 내용이 삭제됩니다.\n그래도 지우시겠습니까?");

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                de.setCurrentMode(Mode.CLEAR);
                sendModeMqttMessage(Mode.CLEAR);
                de.clearDrawingComponents();
                de.clearTexts();
                de.getDrawingFragment().getBinding().redoBtn.setEnabled(false);
                de.getDrawingFragment().getBinding().undoBtn.setEnabled(false);
                invalidate();

                MyLog.i("drawing", "history.size()=" + de.getHistory().size());
                MyLog.i("drawing", "clear");
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MyLog.i("drawing", "canceled");
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void clearBackgroundImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(de.getDrawingFragment().getActivity());
        builder.setTitle("배경 초기화").setMessage("배경 이미지가 삭제됩니다.\n그래도 지우시겠습니까?");

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                de.setCurrentMode(Mode.CLEAR_BACKGROUND_IMAGE);
                sendModeMqttMessage(Mode.CLEAR_BACKGROUND_IMAGE);
                de.setBackgroundImage(null);
                de.clearBackgroundImage();

                MyLog.i("drawing", "clear background image");
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MyLog.i("drawing", "canceled");
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void undo() {
        Mode preMode = de.getCurrentMode();
        de.setCurrentMode(Mode.UNDO);
        sendModeMqttMessage(Mode.UNDO);
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
        sendModeMqttMessage(Mode.REDO);
        de.redo();
        if(de.getHistory().size() == 1)
            de.getDrawingFragment().getBinding().undoBtn.setEnabled(true);

        if(de.getUndoArray().size() == 0)
            de.getDrawingFragment().getBinding().redoBtn.setEnabled(false);

        invalidate();
        de.setCurrentMode(preMode);
    }

    private void showToastMsg(final String message) { Toast.makeText(de.getDrawingFragment().getActivity(), message, Toast.LENGTH_SHORT).show(); }

}
