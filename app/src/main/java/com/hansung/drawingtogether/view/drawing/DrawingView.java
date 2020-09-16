package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;

import androidx.annotation.Nullable;

import java.util.ArrayList;

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
    private boolean movable = false;
    private Canvas drawingViewCanvas;

    private String topicData;

    private DrawingTool dTool = new DrawingTool();
    private Command eraserCommand = new EraseCommand();
    private Command selectCommand = new SelectCommand();

    private boolean isIntercept = false;
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
        MyLog.e("DrawingView", "call onSizeChanged" + " w = " + w + ", h = " + h);

        topicData = client.getTopic_data();
        canvasWidth = w;
        canvasHeight = h;

        if(de.getMainBitmap() == null) {
            de.setMainBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            de.setReceiveBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));

            de.setPreSelectedComponentsBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            de.setPostSelectedComponentsBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            de.setCurrentBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));

            de.setMainCanvas(new Canvas(de.getMainBitmap()));
            de.setReceiveCanvas(new Canvas(de.getReceiveBitmap()));
            de.setCurrentCanvas(new Canvas(de.getCurrentBitmap()));

        }
        if(de.getDrawingBoardArray() == null) {
            de.initDrawingBoardArray(w, h);
            MyLog.i("drawing", "initDrawingBoardArray");
        }
        if(client.isMaster()) {
            MyLog.i("mqtt", "progressDialog dismiss");
            client.getProgressDialog().dismiss();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawingViewCanvas = canvas;

        try {   //중간자 들어오다가 브로커 연결 유실되면 NullPointerException 발생
            canvas.drawBitmap(de.getMainBitmap(), 0, 0, null);
            canvas.drawBitmap(de.getReceiveBitmap(), 0, 0, null);
            canvas.drawBitmap(de.getCurrentBitmap(), 0, 0, null);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        //Log.i("drawing", "onDraw");
        //this.invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!de.isIntercept()) {
            this.isIntercept = false;
            //MyLog.i("intercept", "DrawingView false");
        }
        if(this.isIntercept || ((event.getAction() == MotionEvent.ACTION_DOWN) && de.isIntercept())) {
            //MyLog.i("intercept", "drawing view isIntercept = " + isIntercept + ", de isIntercept = " + de.isIntercept() + ", " + event.getAction());
            MyLog.i("drawing", "intercept drawing view touch 111");
            return true;
        } else {
            this.getParent().requestDisallowInterceptTouchEvent(true);
            //MyLog.i("intercept", "not intercepted action=" + event.getAction());

            setEditorAttribute();

            /*if(de.getCurrentMode() != Mode.SELECT) {
                de.initSelectedBitmap();
            }*/

            switch (de.getCurrentMode()) {
                case DRAW:
                    if((event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP) && !movable) {
                        MyLog.i("drawing", "intercept drawing view touch 222");
                        return true;
                    }

                    if (event.getAction() == MotionEvent.ACTION_DOWN)
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
        //dComponent.setId(de.componentIdCounter());  //id 자동 증가
        dComponent.setUsername(de.getUsername());
        dComponent.setUsersComponentId(de.usersComponentIdCounter());
        dComponent.setType(de.getCurrentType());
        dComponent.setPenMode(de.getPenMode());
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

    public void addPointAndDraw(DrawingComponent dComponent, Point point, Canvas mCanvas) {
        dComponent.addPoint(point);
        dComponent.setBeginPoint(dComponent.getPoints().get(0));
        dComponent.setEndPoint(point);

        dComponent.draw(mCanvas);
    }

    public void addPoint(DrawingComponent dComponent, Point point) {
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

    public void sendModeMqttMessage(Mode mode) {
        MqttMessageFormat messageFormat = new MqttMessageFormat(de.getMyUsername(), mode);
        //client.publish(topicData, parser.jsonWrite(messageFormat));
        sendMqttMessage.putMqttMessage(messageFormat);  //todo minj
    }

    public void sendSelectMqttMessage(boolean isSelected) {
        if(de.getSelectedComponent() == null)
            return;
        sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), Mode.SELECT, isSelected));
    }

    public void doInDrawActionUp(DrawingComponent dComponent, float canvasWidth, float canvasHeight) {
        de.splitPoints(dComponent, canvasWidth, canvasHeight);
        de.addDrawingComponents(dComponent);
        de.addHistory(new DrawingItem(Mode.DRAW, dComponent/*, de.getDrawingBitmap()*/)); // 드로잉 컴포넌트가 생성되면 History 에 저장
        MyLog.i("drawing", "history.size()=" + de.getHistory().size() + ", id=" + dComponent.getId());

        de.removeCurrentComponents(dComponent.getUsersComponentId()); // fixme

        if(de.getHistory().size() == 1)
            de.getDrawingFragment().getBinding().undoBtn.setEnabled(true);

        de.clearUndoArray();

        //if(de.isIntercept()) this.isIntercept = true;   //**

        de.printCurrentComponents("up");
        de.printDrawingComponents("up");

//        // fixme nayeon for performance
//        ArrayList<Point> points = new ArrayList<Point>();
//
//        Log.e("performane", "delivery time origin component points count = " + de.getDrawingComponents().get(0).getPoints().size());
//
//        for(int i=0; i<250; i++)
//            points.add(new Point(de.getDrawingComponents().get(0).getPoints().get(i)));
//
//        de.getDrawingComponents().get(0).setPoints(points);
//
//        for(int i=0; i< 100; i++) {
//            de.getDrawingComponents().add(de.getDrawingComponents().get(0));
//        }
//        Log.e("performane", "delivery time measurement component points count = " + de.getDrawingComponents().get(0).getPoints().size());
//        Log.e("performane", "delivery time measurement component count = " + de.getDrawingComponents().size());

    }

    boolean isExit = false;
    public boolean onTouchDrawMode(MotionEvent event/*, DrawingComponent dComponent*/) {
        Point point;

        if(isExit && event.getAction() != MotionEvent.ACTION_DOWN) {
            MyLog.i("mqtt", "isExit1 = " + isExit);
            return true;
        }

        // 터치가 DrawingView 밖으로 나갔을 때
        if(event.getX()-5 < 0 || event.getY()-5 < 0 || de.getDrawnCanvasWidth()-5 < event.getX() || de.getDrawnCanvasHeight()-5 < event.getY()) {   //fixme 반응이 느려서 임시로 -5
            //MyLog.i("drawing", "id=" + dComponent.getId() + ", username=" + dComponent.getUsername() + ", begin=" + dComponent.getBeginPoint() + ", end=" + dComponent.getEndPoint());
            MyLog.i("drawing", "exit");

            if(dComponent.points.size() == 0)
                return true;

            point = dComponent.getEndPoint();
            addPointAndDraw(dComponent, point, de.getCurrentCanvas());

            de.clearMyCurrentBitmap();
            dComponent.drawComponent(de.getMainCanvas());

            //publish
            if(points.size() != 0) {
                MyLog.i("sendThread", "send move chunk " + points.size() + ", " + points.toString());
                sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), /*de.getUpdatedDrawingComponentId(dComponent), */dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), (ArrayList<Point>)points.clone()/*points.toString()*/, MotionEvent.ACTION_MOVE));
                points.clear();
            }
            sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), /*de.getUpdatedDrawingComponentId(dComponent), */dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), point, MotionEvent.ACTION_UP));

            isExit = true;
            MyLog.i("mqtt", "isExit2 = " + isExit);

            if(de.isIntercept()) {
                this.isIntercept = true;
                MyLog.i("intercept", "drawingview true");
            }

            movable = false;

            invalidate();
            return true;
        }

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isExit = false;
                MyLog.i("mqtt", "isExit3 = " + isExit);

                setComponentAttribute(dComponent);
                point = new Point((int)event.getX(), (int)event.getY());
                //addPoint(dComponent, point);
                //addPointAndDraw(dComponent, point);

                //publish
                //down에서는 DrawingComponent 자체를 보내고, move, up에서는 추가된 점에 관한 정보만 보낸다.
                sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), dComponent, event.getAction()));

                movable = true;

                break;

            case MotionEvent.ACTION_MOVE:
                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point, de.getCurrentCanvas());

                //publish
                points.add(point);
                //MyLog.i("sendThread", points.size() + "");

                if(points.size() == msgChunkSize) {
                    MyLog.i("sendThread", "send move chunk " + points.size() + ", " + points.toString());
                    sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), /*de.getUpdatedDrawingComponentId(dComponent), */dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), (ArrayList<Point>) points.clone()/*points.toString()*/, event.getAction()));
                    points.clear();
                }

                movable = true;

                break;

            case MotionEvent.ACTION_UP:
                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point, de.getCurrentCanvas());

                de.clearMyCurrentBitmap();
                dComponent.drawComponent(de.getMainCanvas());

                //MyLog.i("drawing", "id=" + dComponent.getId() + ", username=" + dComponent.getUsername() + ", begin=" + dComponent.getBeginPoint() + ", end=" + dComponent.getEndPoint());

                //publish
                if(points.size() != 0) {
                    MyLog.i("sendThread", "send move chunk " + points.size() + ", " + points.toString());
                    sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), /*de.getUpdatedDrawingComponentId(dComponent), */dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), (ArrayList<Point>) points.clone()/*points.toString()*/, MotionEvent.ACTION_MOVE));
                    points.clear();
                }
                sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), /*de.getUpdatedDrawingComponentId(dComponent), */dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), point, event.getAction()));

                if(de.isIntercept()) {
                    this.isIntercept = true;
                    MyLog.i("intercept", "drawingview true");
                }

                movable = false;

                break;
            default:
                MyLog.i("drawing", "action = " + MotionEvent.actionToString(event.getAction()));
        }
        invalidate();
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
    int selectMsgChunkSize = 10;
    ArrayList<Point> moveSelectPoints = new ArrayList<>(selectMsgChunkSize);
    int totalMoveX = 0;
    int totalMoveY = 0;
    int preMoveX = 0;
    int preMoveY = 0;
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
                        DrawingComponent comp = de.findDrawingComponentById(selectedComponentId);
                        if((comp != null) && !comp.isSelected()) {
                            isSelected = true;

                            de.setSelectedComponent(comp);
                            if(de.getSelectedComponent() != null) {
                                if(de.getSelectedComponent().isSelected()) {
                                    de.getSelectedComponent().setSelected(false);
                                    de.setDrawingComponentSelected(de.getSelectedComponent().getUsersComponentId(), false);

                                    //todo publish - 다른 사람들 셀렉트 가능 --> 모드 바뀔 때 추가로 메시지 전송 필요
                                    sendSelectMqttMessage(false);
                                }
                            }
                            de.getSelectedComponent().setSelected(true);
                            de.setDrawingComponentSelected(de.getSelectedComponent().getUsersComponentId(), true);

                            String str = "dc(select) [" + de.getDrawingComponents().size() + "] = ";
                            for(DrawingComponent dc: de.getDrawingComponents()) {
                                str += dc.getId() + "(" + dc.getUsersComponentId() + "," + dc.isSelected() + ")" + " ";
                            }
                            MyLog.i("drawing", str);

                            de.setPreSelectedComponents(selectedComponentId);
                            de.setPostSelectedComponents(selectedComponentId);

                            //de.clearMyCurrentBitmap();

                            de.setPreSelectedComponentsBitmap();
                            de.setPostSelectedComponentsBitmap();

                            //de.getSelectedComponent().drawComponent(de.getMyCurrentCanvas());
                            de.drawSelectedComponentBorder(de.getSelectedComponent(), de.getMySelectedBorderColor());
                            invalidate();

                            //select success
                            MyLog.i("drawing", "selected id=" + selectedComponentId);

                            //todo publish - 다른 사람들 셀렉트 못하게
                            sendSelectMqttMessage(true);

                        } else {
                            MyLog.i("drawing", "already selected");
                            showToastMsg("다른 사람이 선택한 도형입니다");
                        }
                    } else {
                        //de.initSelectedBitmap();
                        MyLog.i("drawing", "not selected=" + selectedComponentId);

                        //select cancel
                        if(de.getSelectedComponent() != null) {
                            isSelected = false;
                            de.getSelectedComponent().setSelected(false);
                            de.setDrawingComponentSelected(de.getSelectedComponent().getUsersComponentId(), false);

                            de.clearMyCurrentBitmap();
                            //de.updateDrawingBitmap(false);
                            //de.clearAllSelectedBitmap();
                            //de.clearDrawingBitmap();
                            //de.drawAllDrawingComponents();

                            invalidate();

                            sendSelectMqttMessage(false);
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
                        isSelected = false;
                        de.getSelectedComponent().setSelected(false);
                        MyLog.i("drawing", "selected false");
                        //de.initSelectedBitmap();
                        //de.deselect();

                        de.setDrawingComponentSelected(de.getSelectedComponent().getUsersComponentId(), false);

                        de.clearMyCurrentBitmap();
                        //de.updateDrawingBitmap(false);
                        //de.clearAllSelectedBitmap();
                        //de.clearDrawingBitmap();
                        //de.drawAllDrawingComponents();

                        //todo publish - 다른 사람들 셀렉트 가능
                        sendSelectMqttMessage(false);

                        return true;
                    }

                    //de.clearMyCurrentBitmap();

                    de.setPreSelectedComponentsBitmap();
                    de.setPostSelectedComponentsBitmap();

                    de.clearMyCurrentBitmap();
                    de.drawUnselectedComponents();
                    de.getSelectedComponent().drawComponent(de.getCurrentCanvas());
                    de.drawSelectedComponentBorder(de.getSelectedComponent(), de.getMySelectedBorderColor());
                    invalidate();

                    MyLog.i("drawing", "selected true");

                    totalMoveX = 0;
                    totalMoveY = 0;
                    preMoveX = de.getSelectedComponent().beginPoint.x;
                    preMoveY = de.getSelectedComponent().beginPoint.y;

                    //todo publish - selected down
                    moveSelectPoints.clear();
                    sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), Mode.SELECT, event.getAction(), (ArrayList<Point>)moveSelectPoints.clone()));

                    return true;

                case MotionEvent.ACTION_MOVE:
                    MyLog.i("drawing", "selected move");

                    moveX = (int)(((int)event.getX() - selectDownPoint.x)/de.getSelectedComponent().getXRatio());
                    moveY = (int)(((int)event.getY() - selectDownPoint.y)/de.getSelectedComponent().getYRatio());
                    //moveX = (((int)event.getX() - selectDownPoint.x));
                    //moveY = (((int)event.getY() - selectDownPoint.y));

                    //Point datumPoint = de.getSelectedComponent().getDatumPoint();
                    Point datumPoint = new Point((int)(de.getSelectedComponent().getDatumPoint().x*de.getSelectedComponent().getXRatio()), (int)(de.getSelectedComponent().getDatumPoint().y*de.getSelectedComponent().getYRatio()));

                    int width = de.getSelectedComponent().getWidth();
                    int height = de.getSelectedComponent().getHeight();

                    if((datumPoint.x+moveX-10 < 0 && moveX < 0) || (datumPoint.y+moveY-10 < 0 && moveY < 0) || (datumPoint.y+moveY+height+10 > de.getDrawnCanvasHeight() && moveY > 0) || (datumPoint.x+moveX+width+10 > de.getDrawnCanvasWidth() && moveX > 0)) {
                        return true;
                    }

                    totalMoveX += moveX;
                    totalMoveY += moveY;

                    selectDownPoint = new Point((int)event.getX(), (int)event.getY());

                    de.clearMyCurrentBitmap();
                    de.moveSelectedComponent(de.getSelectedComponent(), moveX, moveY);
                    de.getSelectedComponent().drawComponent(de.getCurrentCanvas());
                    de.drawSelectedComponentBorder(de.getSelectedComponent(), de.getMySelectedBorderColor());
                    invalidate();

                    //todo publish - selected move
                    moveSelectPoints.add(new Point(moveX, moveY));

                    if(moveSelectPoints.size() == selectMsgChunkSize) {
                        MyLog.i("sendThread", "send selected move chunk");
                        sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), Mode.SELECT, event.getAction(), (ArrayList<Point>)moveSelectPoints.clone()));
                        moveSelectPoints.clear();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    MyLog.i("drawing", "selected up");

                    de.updateDrawingBitmap(true);
                    de.splitPointsOfSelectedComponent(de.getSelectedComponent(), de.getMyCanvasWidth(), de.getMyCanvasHeight());
                    de.updateSelectedComponent(de.getSelectedComponent());
                    MyLog.i("drawing", "drawingComponents.size() = " + de.getDrawingComponents().size());

                    if(de.getSelectedComponent().clone() != null) {
                        de.addHistory(new DrawingItem(Mode.SELECT, de.getSelectedComponent().clone(), new Point(totalMoveX, totalMoveY)));
                        MyLog.i("drawing", "history.size()=" + de.getHistory().size() + ", preBeginPoint=(" + preMoveX + "," + preMoveY + "), postBeginPoint-movePoint=(" + (de.getSelectedComponent().getBeginPoint().x - (float) totalMoveX) + "," + (de.getSelectedComponent().getBeginPoint().y - (float) totalMoveY) + "), postBeginPoint=" + de.getSelectedComponent().getBeginPoint().toString());
                    }

                    de.clearUndoArray();
                    invalidate();

                    //if(de.isIntercept()) this.isIntercept = true;   //todo - DRAW mode 외에도 중간자 join 시 intercept 처리

                    //todo publish - selected up
                    if(moveSelectPoints.size() != 0) {
                        MyLog.i("sendThread", "send selected up chunk");
                        sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), Mode.SELECT, MotionEvent.ACTION_MOVE, (ArrayList<Point>) moveSelectPoints.clone()));
                        moveSelectPoints.clear();
                    }
                    sendMqttMessage.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), de.getSelectedComponent().getUsersComponentId(), Mode.SELECT, event.getAction(), (ArrayList<Point>)moveSelectPoints.clone()));


                    return true;
            }
            return true;
        }
    }

    public boolean onTouchWarpMode(MotionEvent event) {
        return false;
    }

    public void clearDrawingView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(de.getDrawingFragment().getActivity());
        builder.setTitle("화면 초기화").setMessage("모든 그리기 내용이 삭제됩니다.\n그래도 지우시겠습니까?");

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                de.initSelectedBitmap();

                //de.setCurrentMode(Mode.CLEAR);
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

    public void clear() {
        AlertDialog.Builder builder = new AlertDialog.Builder(de.getDrawingFragment().getActivity());
        builder.setTitle("전체 초기화").setMessage("모든 내용이 삭제됩니다.\n그래도 지우시겠습니까?");

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                de.initSelectedBitmap();

                sendModeMqttMessage(Mode.CLEAR);
                de.clearDrawingComponents();
                de.clearTexts();
                de.getDrawingFragment().getBinding().redoBtn.setEnabled(false);
                de.getDrawingFragment().getBinding().undoBtn.setEnabled(false);
                invalidate();

                de.setCurrentMode(Mode.CLEAR_BACKGROUND_IMAGE);
                sendModeMqttMessage(Mode.CLEAR_BACKGROUND_IMAGE);
                de.setBackgroundImage(null);
                de.clearBackgroundImage();

                MyLog.i("drawing", "clear all");
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
        de.initSelectedBitmap();

        //Mode preMode = de.getCurrentMode();
        //de.setCurrentMode(Mode.UNDO);
        sendModeMqttMessage(Mode.UNDO);
        de.undo();

        if(de.getUndoArray().size() == 1)
            de.getDrawingFragment().getBinding().redoBtn.setEnabled(true);

        if(de.getHistory().size() == 0)
            de.getDrawingFragment().getBinding().undoBtn.setEnabled(false);

        invalidate();
        //de.setCurrentMode(preMode);
    }

    public void redo() {
        de.initSelectedBitmap();

        //Mode preMode = de.getCurrentMode();
        //de.setCurrentMode(Mode.REDO);
        sendModeMqttMessage(Mode.REDO);
        de.redo();
        if(de.getHistory().size() == 1)
            de.getDrawingFragment().getBinding().undoBtn.setEnabled(true);

        if(de.getUndoArray().size() == 0)
            de.getDrawingFragment().getBinding().redoBtn.setEnabled(false);

        invalidate();
        ///de.setCurrentMode(preMode);
    }

    private void showToastMsg(final String message) { Toast.makeText(de.getDrawingFragment().getActivity(), message, Toast.LENGTH_SHORT).show(); }

}
