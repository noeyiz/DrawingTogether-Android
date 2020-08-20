package com.hansung.drawingtogether.view.drawing;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;

import com.hansung.drawingtogether.data.remote.model.MyLog;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import lombok.Getter;

@Getter
public enum DrawingEditor {
    INSTANCE;

    private DrawingFragment drawingFragment;

    /* 배경 이미지와 그리기 위한 자원 */
    private Bitmap backgroundImage;             //
    private Bitmap drawingBitmap;               //그리기 bitmap
    private Canvas backCanvas;                  //미리 그려두기 위한 Canvas
    private Bitmap lastDrawingBitmap = null;    //drawingBitmap 의 마지막 상태 bitmap --> 도형 그리기

    /* 드로잉 컴포넌트에 필요한 객체 */
    private boolean isIntercept = false;
    //private int currentDrawAction = MotionEvent.ACTION_UP;
    private boolean isDrawingShape = false;
    private int componentId = -1;
    private int maxComponentId = -1;
    private Vector<Integer> removedComponentId = new Vector<>();
    private Vector<Integer>[][] drawingBoardArray = null;
    private SparseArray<ArrayList<Point>> drawingBoardMap = new SparseArray<>();

    private ArrayList<DrawingComponent> drawingComponents = new ArrayList<>();  //현재 그려져있는 모든 drawing component 배열
    private ArrayList<DrawingComponent> currentComponents = new ArrayList<>();  //현재 그리기중인 drawing component 배열
    private ArrayList<DrawingComponent> currentShapes = new ArrayList<>();      //현재 그리기중인 shape drawing component 배열

    /* selector 에 필요한 객체 */
    private DrawingComponent selectedComponent = null;
    private ArrayList<DrawingComponent> preSelectedComponents = new ArrayList<>();
    private ArrayList<DrawingComponent> postSelectedComponents = new ArrayList<>();
    private Bitmap selectedComponentBitmap;
    private Bitmap preSelectedComponentsBitmap;
    private Bitmap postSelectedComponentsBitmap;
    private int selectedBorderColor = Color.LTGRAY;
    private int mySelectedBorderColor = Color.GRAY;

    /* 텍스트에 필요한 객체 */
    private Drawable textMoveBorderDrawable; // 텍스트 움직일 때 테두리
    private Drawable textFocusBorderDrawable; // 다른 사용자 사용 텍스트 표시
    private Drawable textHighlightBorderDrawble; // 텍스트 색상 변경 시 사용 표시 // fixme nayeon
    private ArrayList<Text> texts = new ArrayList<>(); // 현재 부착된 모든 text 배열
    private Text currentText = null;
    private boolean isTextBeingEdited = false;
    private int textId = -1;
    private int maxTextId = -1;

    private boolean isMidEntered = false; // fixme nayeon 텍스트 중간자 처리를 위한 플래그

    /* UNDO, REDO 를 위한 객체 */
    private ArrayList<DrawingItem> history = new ArrayList<>();     //
    private ArrayList<DrawingItem> undoArray = new ArrayList<>();   //undo 배열

    /* 드로잉 컴포넌트 속성 */
    private Mode currentMode/* = Mode.DRAW*/;
    private ComponentType currentType/* = ComponentType.STROKE*/;
    private String myUsername;
    private String username;
    private float drawnCanvasWidth;
    private float drawnCanvasHeight;
    private float myCanvasWidth;
    private float myCanvasHeight;

    /* 드로잉 펜 속성 */
    private String fillColor = "#000000";
    private String strokeColor = "#000000";
    private int strokeAlpha = 255;
    private int fillAlpha = 100;
    private int strokeWidth = 10;

    /* 텍스트 속성 */
    private int textSize = 20;
    private String textColor = "#000000";
    private int fontStyle = Typeface.BOLD;
    private int textBackground = Color.TRANSPARENT;

    // 드로잉 하는동안 저장되는 모든 데이터들 지우기 [나가기 버튼 눌렀을 때 처리 필요 - MQTTClient.java if(topic_exit, topic_delete) 부분에서 호출]
    public void removeAllDrawingData() {
        backgroundImage = null;
        drawingBitmap = null;
        lastDrawingBitmap = null;

        componentId = -1;
        maxComponentId = -1;
        removedComponentId.clear();
        clearDrawingBoardArray();
        drawingBoardMap.clear();

        drawingComponents.clear();
        currentComponents.clear();

        //removeAllTextViewToFrameLayout();
        texts.clear();
        currentText = null;
        isTextBeingEdited = false;
        textId = -1;

        history.clear();
        undoArray.clear();

        currentMode = Mode.DRAW;
        currentType = ComponentType.STROKE;
        strokeColor = "#000000";
        strokeWidth = 10;

        isIntercept = false;
        isMidEntered = false;
    }

    public void printDrawingData() {
        MyLog.i("backgroundImage", backgroundImage.toString());
        MyLog.i("drawingBitmap", drawingBitmap.toString());
        MyLog.i("lastDrawingBitmap", lastDrawingBitmap.toString());

        MyLog.i("componentId", Integer.toString(componentId));
        MyLog.i("maxComponentId", Integer.toString(maxComponentId));
        MyLog.i("removedComponentId", Integer.toString(removedComponentId.size()));
        MyLog.i("drawingBoardArray", Integer.toString(drawingBoardArray.length));
        MyLog.i("drawingBoardMap", Integer.toString(drawingBoardArray.length));

        MyLog.i("drawingComponents", Integer.toString(drawingComponents.size()));
        MyLog.i("currentComponents", Integer.toString(currentComponents.size()));

        MyLog.i("texts", Integer.toString(texts.size()));
        MyLog.i("currentText", currentText.toString());
        MyLog.i("isTextBeingEdited", Boolean.toString(isTextBeingEdited));

        MyLog.i("history", Integer.toString(history.size()));
        MyLog.i("undoArray", Integer.toString(undoArray.size()));
    }

    public static DrawingEditor getInstance() { return INSTANCE; }

    public void drawAllDrawingComponents() {   //drawingComponents draw
        for (DrawingComponent drawingComponent : drawingComponents) {
            drawingComponent.drawComponent(getBackCanvas());
        }
        /*for (DrawingComponent currentComponent : currentComponents) {
            MyLog.i("drawing", "cc.size() = " + currentComponents.size());
            currentComponent.drawComponent(getBackCanvas());
        }*/
    }

    public void drawAllUnselectedDrawingComponents() {   //drawingComponents draw
        for (DrawingComponent drawingComponent : drawingComponents) {
            if(!drawingComponent.isSelected())
                drawingComponent.drawComponent(getBackCanvas());
        }
        /*for (DrawingComponent currentComponent : currentComponents) {
            MyLog.i("drawing", "cc.size() = " + currentComponents.size());
            currentComponent.drawComponent(getBackCanvas());
        }*/
    }

    public void drawAllCurrentStrokes() {   //drawingComponents draw
        for (DrawingComponent component : currentComponents) {
            if(component.getType() == ComponentType.STROKE) {

                component.drawComponent(getBackCanvas());
                if(component.username.equals(this.myUsername))
                    drawingFragment.getBinding().drawingView.drawDComponent();
            }
        }
    }

    public void drawAllDrawingComponentsForMid() {   //drawingComponents draw
        for (DrawingComponent component : drawingComponents) {

            component.calculateRatio(drawingBoardArray[0].length, drawingBoardArray.length);
            component.drawComponent(getBackCanvas());

            splitPoints(component, drawingBoardArray[0].length, drawingBoardArray.length);
        }

        MyLog.i("drawing", "drawingBoardArray[][] w=" + drawingBoardArray[0].length + ", h=" + drawingBoardArray.length);
        MyLog.i("drawing", "dba[0][0] = " + drawingBoardArray[0][0].get(0));
    }

    public void addCurrentComponents(DrawingComponent component) {
        this.currentComponents.add(component);
    }

    public void removeCurrentComponents(String id) {
        MyLog.i("drawing", "remove " + id);
        for(DrawingComponent component: currentComponents) {
            if(component.getUsersComponentId().equals(id)) {
                //MyLog.i("drawing", "remove success cc " + component.getUsersComponentId());
                currentComponents.remove(component);
                break;
            }
        }
    }

    public DrawingComponent getCurrentComponent(String usersComponentId) {
        for(DrawingComponent component: currentComponents) {
            if(component.getUsersComponentId().equals(usersComponentId))
                return component;
        }
        return null;
    }

    public void printCurrentComponents(String status) {
        String str = "cc(" + status + ") [" + getCurrentComponents().size() + "] = ";
        for(DrawingComponent dc: getCurrentComponents()) {
            str += dc.getId() + "(" + dc.getUsersComponentId() + ")" + " ";
        }
        MyLog.i("drawing", str);
    }

    public boolean isContainsCurrentComponents(int id, String usersComponentId) {    //다른 디바이스에서 동시에 그렸을 경우
        printCurrentComponents("contains");

        for(DrawingComponent component: currentComponents) {
            if(component.getId() == id && !component.getUsersComponentId().equals(usersComponentId))
                return true;
        }
        return false;
    }

    public void addDrawingComponents(DrawingComponent component) {
        this.drawingComponents.add(component);
    }

    public void sortDrawingComponents() {
        Collections.sort(this.drawingComponents, new Comparator<DrawingComponent>() {
            @Override
            public int compare(DrawingComponent o1, DrawingComponent o2) {
                if(o1.getId() > o2.getId())
                    return 1;
                else
                    return -1;
                //return 0;
            }
        });
        printDrawingComponents("sort");
    }

    public void addAllDrawingComponents(Vector<DrawingComponent> components) {
        this.drawingComponents.addAll(components);
    }

    public void removeAllDrawingComponents(Vector<Integer> ids) {
        for(int i: ids)
            removeDrawingComponents(i);
    }

    public int removeDrawingComponents(int id) {
        for(int i=0; i<drawingComponents.size(); i++) {
            if(drawingComponents.get(i).getId() == id) {
                /*if(drawingComponents.get(i).isSelected()) {
                    drawingComponents.get(i).setSelected(false);
                }*/
                drawingComponents.remove(drawingComponents.get(i));
                return i;
            }
        }
        return -1;
    }

    public void printDrawingComponents(String status) {
        String str = "dc(" + status + ") [" + getDrawingComponents().size() + "] = ";
        for(DrawingComponent dc: getDrawingComponents()) {
            str += dc.getId() + "(" + dc.getUsersComponentId() + ")" + " ";
        }
        MyLog.i("drawing", str);
    }

    public boolean isContainsAllDrawingComponents(Vector<Integer> ids) {
        for(int i: ids) {
            if(!isContainsDrawingComponents(i))
                return false;
        }
        return true;
    }

    public boolean isContainsDrawingComponents(int id) {
        printDrawingComponents("contains");

        for(DrawingComponent component: drawingComponents) {
            if(component.getId() == id)
                return true;
        }
        return false;
    }

    public boolean isContainsDrawingComponents(int id, String usersComponentId) {
        printDrawingComponents("contains");

        for(DrawingComponent component: drawingComponents) {
            if(component.getId() == id && !component.getUsersComponentId().equals(usersComponentId))
                return true;
        }
        return false;
    }

    public int getIndexOfDrawingComponent(int id) {
        for(int i=0; i<drawingComponents.size(); i++) {
            if(drawingComponents.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    public void updateDrawingComponents(DrawingComponent newComponent) {    //속성 변경 update
        int index = getIndexOfDrawingComponent(newComponent.getId());

        for(int i=0; i<drawingComponents.size(); i++) {
            if(drawingComponents.get(i).getId() == newComponent.getId()) {
                drawingComponents.remove(drawingComponents.get(i));
            }
        }

        drawingComponents.add(index, newComponent);
        //addDrawingComponents(newComponent);
    }

    public void addCurrentShapes(DrawingComponent component) {
        this.currentShapes.add(component);
    }

    public int removeCurrentShapes(String usersComponentId) {
        for(int i=0; i<currentShapes.size(); i++) {
            if(currentShapes.get(i).getUsersComponentId().equals(usersComponentId)) {
                currentShapes.remove(i);
                return i;
            }
        }
        return -1;
    }

    public void updateCurrentShapes(DrawingComponent newComponent) {
        int index = removeCurrentShapes(newComponent.getUsersComponentId());
        if(index == -1) return;
        currentShapes.add(index, newComponent);
    }

    public void deselect() {
        if(selectedComponent == null) return;
        drawingFragment.getBinding().drawingView.setSelected(false);
        try {
            findDrawingComponentByUsersComponentId(selectedComponent.getUsersComponentId()).setSelected(false);
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
        selectedComponent.setSelected(false);
        //selectedComponent = null;
        updateDrawingBitmap(false);
        //clearAllSelectedBitmap();
        //drawingFragment.getBinding().drawingView.invalidate();
        setLastDrawingBitmap(drawingBitmap.copy(drawingBitmap.getConfig(), true));
    }

    public void initSelectedBitmap() {
        if(drawingFragment.getBinding().drawingView.isSelected) {
            deselect();
            clearAllSelectedBitmap();
            drawingFragment.getBinding().drawingView.invalidate();
            drawingFragment.getBinding().drawingView.sendSelectMqttMessage(false);
        }
    }

    public void setPreSelectedComponents(int id) {
        preSelectedComponents.clear();
        for(DrawingComponent component: drawingComponents) {
            if(component.getId() < id)
                preSelectedComponents.add(component);
        }
        String str = "pre selected = ";
        for(DrawingComponent component: preSelectedComponents) {
            str += component.getId() + " ";
        }
        MyLog.i("drawing", str);
    }

    public void setPostSelectedComponents(int id) {
        postSelectedComponents.clear();
        tempPostSelectedComponents.clear();
        for(DrawingComponent component: drawingComponents) {
            if(id < component.getId())
                postSelectedComponents.add(component);
        }
        String str = "post selected = ";
        for(DrawingComponent component: postSelectedComponents) {
            str += component.getId() + " ";
        }
        MyLog.i("drawing", str);
    }

    public boolean isContainsSelectedComponent(Point point) {
        try {
            if (selectedComponent.getType() == ComponentType.STROKE) return false;

            //Point datumPoint = selectedComponent.getDatumPoint();
            Point datumPoint = new Point((int)(selectedComponent.getDatumPoint().x*selectedComponent.getXRatio()), (int)(selectedComponent.getDatumPoint().y*selectedComponent.getYRatio()));

            int width = selectedComponent.getWidth();
            int height = selectedComponent.getHeight();
            if ((datumPoint.x <= point.x && point.x <= datumPoint.x + width) && (datumPoint.y <= point.y && point.y <= datumPoint.y + height))
                return true;

        } catch(NullPointerException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void drawSelectedComponentBorder(DrawingComponent component, int color) {
        //component.calculateRatio(canvasWidth, canvasHeight);
        Point datumPoint = component.getDatumPoint();
        int width = component.getWidth();
        int height = component.getHeight();
        int strokeWidth = component.getStrokeWidth();

        //MyLog.i("drawing", "draw selected component border");

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        try {
            DashPathEffect dashPath = new DashPathEffect(new float[]{12, 4}, 4);
            paint.setPathEffect(dashPath);
            paint.setStrokeWidth(4);
            //paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);     //윤곽선
            paint.setColor(color);
            if(getSelectedCanvas() == null) {
                selectedComponentBitmap.eraseColor(Color.TRANSPARENT);
                selectedCanvas = new Canvas(selectedComponentBitmap);
            }
            getSelectedCanvas().drawRect((datumPoint.x*component.getXRatio() - strokeWidth / 2 - 10), (datumPoint.y*component.getYRatio() - strokeWidth / 2 - 10), (datumPoint.x*component.getXRatio() + width + strokeWidth / 2 + 10), (datumPoint.y*component.getYRatio() + height + strokeWidth / 2 + 10), paint);

        }catch(NullPointerException e) {
            e.printStackTrace();
        }
    }

    private Canvas selectedCanvas;
    public void drawSelectedComponent() {
        selectedComponentBitmap.eraseColor(Color.TRANSPARENT);
        selectedCanvas = new Canvas(selectedComponentBitmap);
        selectedComponent.draw(selectedCanvas);
    }

    public void drawAllPreSelectedComponents() {
        preSelectedComponentsBitmap.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(preSelectedComponentsBitmap);
        for(DrawingComponent component: preSelectedComponents) {
            component.drawComponent(canvas);
        }
    }

    public void drawAllPostSelectedComponents() {
        postSelectedComponents.addAll(tempPostSelectedComponents);
        postSelectedComponentsBitmap.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(postSelectedComponentsBitmap);
        for(DrawingComponent component: postSelectedComponents) {
            component.drawComponent(canvas);
        }
    }

    ArrayList<DrawingComponent> tempPostSelectedComponents = new ArrayList<>();
    public void addPostSelectedComponent(DrawingComponent component) {
        tempPostSelectedComponents.add(component);
        Canvas canvas = new Canvas(postSelectedComponentsBitmap);
        component.drawComponent(canvas);
    }

    public void drawSelectedBitmaps() {
        drawingBitmap.eraseColor(Color.TRANSPARENT);
        drawingBitmap = preSelectedComponentsBitmap.copy(preSelectedComponentsBitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);
        backCanvas.drawBitmap(postSelectedComponentsBitmap, 0, 0, null);
    }

    public void moveSelectedComponent(DrawingComponent selectedComponent, int moveX, int moveY) {
        selectedComponent.setBeginPoint(new Point(selectedComponent.getBeginPoint().x + moveX, selectedComponent.getBeginPoint().y + moveY));
        selectedComponent.setEndPoint(new Point(selectedComponent.getEndPoint().x + moveX, selectedComponent.getEndPoint().y + moveY));
        selectedComponent.setDatumPoint(new Point(selectedComponent.getDatumPoint().x + moveX/*(int)(moveX*selectedComponent.getXRatio())*/, selectedComponent.getDatumPoint().y + moveY/*(int)(moveY*selectedComponent.getYRatio())*/));
    }

    public void updateDrawingBitmap(boolean border) {
        drawingBitmap.eraseColor(Color.TRANSPARENT);
        drawingBitmap = preSelectedComponentsBitmap.copy(preSelectedComponentsBitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);
        clearSelectedBitmap();
        selectedComponent.drawComponent(backCanvas);
        if(border) {
            drawSelectedComponentBorder(getSelectedComponent(), mySelectedBorderColor); //
        }
        backCanvas.drawBitmap(postSelectedComponentsBitmap, 0, 0, null);
    }

    public void updateSelectedComponent(DrawingComponent component, float canvasWidth, float canvasHeight) {
        if(component == null) return;

        Vector<Integer> id = new Vector<>();
        id.add(-1);
        id.add(component.getId());
        eraseDrawingBoardArray(id);

        component.calculateRatio(canvasWidth, canvasHeight);
        ArrayList<Point> newPoints = new ArrayList<>();

        try{
            if (component.getType() == ComponentType.STROKE) return;

            Point datumPoint = new Point((int)(component.getDatumPoint().x*component.getXRatio()), (int)(component.getDatumPoint().y*component.getYRatio()));
            int width = component.getWidth();
            int height = component.getHeight();

            for(int i=datumPoint.y; i<=datumPoint.y + height; i++) {
                newPoints.add(new Point(datumPoint.x, i));
                newPoints.add(new Point(datumPoint.x + width, i));
            }
            for(int i=datumPoint.x; i<=datumPoint.x + width; i++) {
                newPoints.add(new Point(i, datumPoint.y));
                newPoints.add(new Point(i, datumPoint.y + height));
            }

        } catch(NullPointerException e) {
            e.printStackTrace();
        }

        drawingBoardMap.put(component.getId(), newPoints);

        for(Point point: newPoints) {
            int x = point.x;
            int y = point.y;

            if(!drawingBoardArray[y][x].contains(component.getId()))
                drawingBoardArray[y][x].add(component.getId());
        }
    }

    public void addTexts(Text text) {
        this.texts.add(text);
    }

    public void removeTexts(Text text) { this.texts.remove(text); }

    public Text findTextById(String id) {
        for(Text text: texts) {
            if(text.getTextAttribute().getId().equals(id)) {
                return text;
            }
        }
        return null;
    }

    //fixme nayeon - [ 텍스트 아이디 = "사용자이름-textCount" ] 동시성 처리 필요 X
    public String setTextStringId() { return myUsername + "-" + textIdCounter(); }

    public void removeAllTextViewToFrameLayout() {
        for(Text t: texts) {
            t.removeTextViewToFrameLayout();
        }
    }

    public void addAllTextViewToFrameLayoutForMid() {
        for(Text t: texts) {
            // fixme nayeon
            // 다른 사용자(마스터)가 편집중일 텍스트일 경우 , TextAttribute 의 String text 는 계속해서 변하는 중
            // 그리고 텍스트 테두리 설정 안 되어 있음
            if(t.getTextAttribute().getUsername() != null) {
                t.getTextView().setText(t.getTextAttribute().getPreText()); // 이전 텍스트로 설정
                t.getTextView().setBackground(this.textFocusBorderDrawable); // 테두리 설정
            }
            // 중간에 들어왔는데 색상

            // fixme nayeon
            t.setTextViewInitialPlace(t.getTextAttribute());
            t.setTextViewProperties();

            t.addTextViewToFrameLayout();
            t.createGestureDetector(); // 텍스트 모두 붙이기를 중간자 처리, 재접속 시에만 한다고 가정했을 때.

            MyLog.e("texts size, text id", texts.size() + ", " + t.getTextAttribute().getId());
            MyLog.e("text view size", t.getTextView().getWidth() + ", " + t.getTextView().getHeight());
            MyLog.e("text view location", t.getTextView().getX() + ", " + t.getTextView().getY());
        }
    }

    public int textIdCounter() {
        maxTextId = ++textId;
        return maxTextId;
    }

    public void addRemovedComponentIds(Vector<Integer> ids) {
        for(int i: ids) {
            if(!removedComponentId.contains(i))
                removedComponentId.add(i);
        }
    }

    public void removeRemovedComponentIds(Vector<Integer> ids) {
        for(int i: ids) {
            if(removedComponentId.contains(i))
                removedComponentId.removeElement(i);
        }
    }

    public Vector<Integer> getNotRemovedComponentIds(Vector<Integer> ids) {
        Vector<Integer> temp = new Vector<>();
        for(int i=0; i<ids.size(); i++) {
            if (!removedComponentId.contains(ids.get(i)))
                temp.add(ids.get(i));
        }
        return temp;
    }

    public boolean isContainsRemovedComponentIds(Vector<Integer> ids) {
        boolean flag = true;
        for(int i=1; i<ids.size(); i++) {
            if(!removedComponentId.contains(ids.get(i))) {
                flag = false;
            }
        }
        return flag;
    }

    public void addHistory(DrawingItem item) {
        history.add(item);
    }

    public void addUndoArray(DrawingItem item) {
        undoArray.add(item);
    }

    public void updateDrawingItem(DrawingItem lastItem) {
        if(lastItem.getComponents() == null)
            return;

        MyLog.i("drawing", "mode = " + lastItem.getMode().toString());

        Vector<Integer> ids = new Vector<>();
        for (DrawingComponent component: lastItem.getComponents()) {
            ids.add(component.getId());
        }
        MyLog.i("drawing", "last item ids = " + ids.toString());

        /*StringBuilder str = new StringBuilder("dc = ");
        for(DrawingComponent component: getDrawingComponents()) {
            str.append(component.getId()).append(" ");
        }
        MyLog.i("drawing", str.toString());*/

        switch(lastItem.getMode()) {
            case DRAW:
            case ERASE:
                if(isContainsAllDrawingComponents(ids)) {                           //erase
                    MyLog.i("drawing", "update erase");
                    clearDrawingBitmap();
                    addRemovedComponentIds(ids);
                    removeAllDrawingComponents(ids);
                    drawAllDrawingComponents();
                    drawAllCurrentStrokes();
                    eraseDrawingBoardArray(ids);
                } else {
                    MyLog.i("drawing", "update draw");
                    for (DrawingComponent component: lastItem.getComponents()) {    //draw
                        component.calculateRatio(myCanvasWidth, myCanvasHeight);
                        //component.drawComponent(getBackCanvas());
                        splitPoints(component, myCanvasWidth, myCanvasHeight);
                        //todo minj - item 이후 그려진 것들 다시 그리기
                        //component.setIsErased(false);
                    }
                    removeRemovedComponentIds(ids);
                    addAllDrawingComponents(lastItem.getComponents());
                    sortDrawingComponents();
                    clearDrawingBitmap();
                    drawAllDrawingComponents();
                    drawAllCurrentStrokes();
                }
                setLastDrawingBitmap(getDrawingBitmap().copy(getDrawingBitmap().getConfig(), true));
                MyLog.i("drawing", "removedComponentIds = " + getRemovedComponentId());

                break;

            case TEXT:  //text mode인 동안 다른 사람이 드로잉 했을 시

            case SELECT:
            case GROUP:
                break;
        }
    }

    public DrawingItem popHistory() {   //undo
        int index = history.size() - 1;
        DrawingItem lastItem = history.get(index);
        history.remove(index);

        try {
            updateLastItem(lastItem, true);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return lastItem;
    }

    public DrawingItem popUndoArray() {  //redo
        int index = undoArray.size() - 1;
        DrawingItem lastItem = undoArray.get(index);

        try {
            updateLastItem(lastItem, false);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        undoArray.remove(index);

        return lastItem;
    }

    public void updateLastItem(DrawingItem lastItem, boolean isUndo) {
        if(lastItem.getMode() != null)
            updateDrawingItem(lastItem);
        else if(lastItem.getTextMode() != null) {
            new UpdateTextsTask(isUndo).execute(lastItem);
        }
    }

    public void clearUndoArray() {
        undoArray.clear();
        drawingFragment.getBinding().redoBtn.setEnabled(false);
    }   //redo 방지

    public DrawingComponent findDrawingComponentById(int id) {
        for(DrawingComponent component: drawingComponents) {
            if(component.getId() == id) {
                return component;
            }
        }
        return null;
    }

    public DrawingComponent findDrawingComponentByUsersComponentId(String usersComponentId) {
        for(DrawingComponent component: drawingComponents) {
            if(component.getUsersComponentId().equals(usersComponentId)) {
                return component;
            }
        }
        return null;
    }

    public int componentIdCounter() {
        //maxComponentId = ++componentId;
        return ++maxComponentId;
    }

    public String usersComponentIdCounter() {
        componentId = ++componentId;
        return this.username + "-" + componentId;
    }

    public void initDrawingBoardArray(int width, int height) {
        MyLog.i("drawing", "initDrawingBoardArray()");
        try{
            drawingBoardArray = new Vector[height][width];

            for(int i=0; i<height; i++) {
                for(int j=0; j<width; j++) {
                    drawingBoardArray[i][j] = new Vector<>();
                    drawingBoardArray[i][j].add(-1);
                }
            }
        } catch(OutOfMemoryError e) {
            MyLog.i("mqtt", "Out of Memory Error at initDrawingBoardArray()");
            e.printStackTrace();
        }
    }

    public void clearDrawingBoardArray() {
        try {
            for (int i = 0; i < drawingBoardArray.length; i++) {
                for (int j = 0; j < drawingBoardArray[i].length; j++) {
                    if(drawingBoardArray[i][j].size() != 1) {
                        drawingBoardArray[i][j].removeAllElements();
                        drawingBoardArray[i][j].add(-1);
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            MyLog.i("mqtt", "Out of Memory Error at clearDrawingBoardArray()");
            e.printStackTrace();
        }
    }

    public ArrayList<Point> getNewPointsById(int id) {
        return drawingBoardMap.get(id);
    }

    //drawingComponent 점 펼치기 --> drawingBoardArray
    public void splitPoints(DrawingComponent component, float canvasWidth, float canvasHeight) {
        if(component == null) return;

        component.calculateRatio(canvasWidth, canvasHeight);
        ArrayList<Point> newPoints = new ArrayList<>();

        try{
            switch(component.getType()) {
                case STROKE:
                    newPoints = strokeSplitPoints(component);
                    break;

                case RECT:  //직사각형 형태 모두 가능 --> rect, text, image
                case OVAL:  //정교하게 수정 x^2/a^2 + y^2/b^2 <= 1
                    newPoints = rectSplitPoints(component);
                    break;
            }
        } catch(NullPointerException e) {   //exit 제대로 안됐을 때, mid 그리기 시
            e.printStackTrace();
        }

        drawingBoardMap.put(component.getId(), newPoints);

        for(Point point: newPoints) {
            int x = point.x;
            int y = point.y;

            if(!drawingBoardArray[y][x].contains(component.getId()))
                drawingBoardArray[y][x].add(component.getId());
        }
        //Log.i("drawing", "newPoints = " + newPoints.toString());
    }

    public ArrayList<Point> strokeSplitPoints(DrawingComponent component) {

        ArrayList<Point> calcPoints = new ArrayList<>();    //화면 비율 보정한 Point 배열
        for(Point point: component.getPoints()) {
            int x = point.x;
            int y = point.y;
            calcPoints.add(new Point((int)(x * component.getXRatio()), (int)(y * component.getYRatio())));
        }
        MyLog.i("drawing", "calcPoints = " + calcPoints.toString());

        ArrayList<Point> newPoints = new ArrayList<>();     //사이 점 채워진 Point 배열
        float slope;       //기울기
        float yIntercept;  //y절편

        for(int i=0; i<calcPoints.size() - 1; i++) {
            Point from = calcPoints.get(i);
            Point to = calcPoints.get(i+1);

            slope = (to.x - from.x) == 0 ? 0 : (to.y - from.y) / (float)(to.x - from.x);
            yIntercept = (from.y - (slope * from.x));

            if(from.x <= to.x) {
                for(int x=from.x; x<=to.x; x++) {
                    int y = (int)((slope * x) + yIntercept);
                    newPoints.add(new Point(x, y));
                }
            } else {
                for(int x=to.x; x<=from.x; x++) {
                    int y = (int)((slope * x) + yIntercept);
                    newPoints.add(new Point(x, y));
                }
            }
        }

        return newPoints;
    }

    public ArrayList<Point> rectSplitPoints(DrawingComponent component) {   //테두리만

        Point calcBeginPoint = new Point((int)(component.getBeginPoint().x * component.getXRatio()), (int)(component.getBeginPoint().y * component.getYRatio()));
        Point calcEndPoint = new Point((int)(component.getEndPoint().x * component.getXRatio()), (int)(component.getEndPoint().y * component.getYRatio()));
        MyLog.i("drawing", "calcBegin = " + calcBeginPoint.toString() + ", calcEnd = " + calcEndPoint.toString());

        int width = Math.abs(calcEndPoint.x - calcBeginPoint.x);
        int height = Math.abs(calcEndPoint.y - calcBeginPoint.y);

        Point datumPoint = (calcBeginPoint.x < calcEndPoint.x) ? calcBeginPoint : calcEndPoint; //기준점 (사각형의 왼쪽위 꼭짓점)

        ArrayList<Point> newPoints = new ArrayList<>();     //사이 점 채워진 Point 배열
        float slope = (calcEndPoint.x - calcBeginPoint.x) == 0 ? 0 : (calcEndPoint.y - calcBeginPoint.y) / (float)(calcEndPoint.x - calcBeginPoint.x);

        if(slope == 0) {
            newPoints.add(calcBeginPoint);
        } else if(slope < 0) {
            datumPoint.y -= height;
        }

        //component.setBeginPoint(calcBeginPoint);
        //component.setEndPoint(calcEndPoint);
        component.setDatumPoint(new Point((int)(datumPoint.x / component.getXRatio()), (int)(datumPoint.y / component.getYRatio())));
        component.setWidth(width);
        component.setHeight(height);

        for(int i=datumPoint.y; i<=datumPoint.y + height; i++) {
            newPoints.add(new Point(datumPoint.x, i));
            newPoints.add(new Point(datumPoint.x + width, i));
        }
        for(int i=datumPoint.x; i<=datumPoint.x + width; i++) {
            newPoints.add(new Point(i, datumPoint.y));
            newPoints.add(new Point(i, datumPoint.y + height));
        }

        //Log.i("drawing", newPoints.toString());
        return newPoints;
    }

    public Vector<Integer> findEnclosingDrawingComponents(Point point) {
        Vector<Integer> erasedComponentIds = new Vector<>();
        erasedComponentIds.add(-1);
        try {
            for (DrawingComponent component : drawingComponents) {
                switch (component.getType()) { // todo nayeon [ int com.hansung.drawingtogether.view.drawing.ComponentType.ordinal() null ] -> DrawingComponent 배열에 ComponentType Null 이 들어가나 확인
                    case STROKE:
                        break;

                    case RECT:
                    case OVAL:
                        //Point datumPoint = component.getDatumPoint();
                        Point datumPoint = new Point((int)(component.getDatumPoint().x*component.getXRatio()), (int)(component.getDatumPoint().y*component.getYRatio()));

                        int width = component.getWidth();
                        int height = component.getHeight();
                        if ((datumPoint.x <= point.x && point.x <= datumPoint.x + width) && (datumPoint.y <= point.y && point.y <= datumPoint.y + height))
                            erasedComponentIds.add(component.getId());
                }
            }
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
        return erasedComponentIds;
    }

    public Vector<Integer> findUnselectedEnclosingDrawingComponents(Point point) {
        Vector<Integer> erasedComponentIds = new Vector<>();
        erasedComponentIds.add(-1);
        try {
            for (DrawingComponent component : drawingComponents) {
                if(!component.isSelected()) {
                    switch (component.getType()) { // todo nayeon [ int com.hansung.drawingtogether.view.drawing.ComponentType.ordinal() null ] -> DrawingComponent 배열에 ComponentType Null 이 들어가나 확인
                        case STROKE:
                            break;

                        case RECT:
                        case OVAL:
                            //Point datumPoint = component.getDatumPoint();
                            Point datumPoint = new Point((int) (component.getDatumPoint().x * component.getXRatio()), (int) (component.getDatumPoint().y * component.getYRatio()));

                            int width = component.getWidth();
                            int height = component.getHeight();
                            if ((datumPoint.x <= point.x && point.x <= datumPoint.x + width) && (datumPoint.y <= point.y && point.y <= datumPoint.y + height))
                                erasedComponentIds.add(component.getId());
                    }
                }
            }
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
        return erasedComponentIds;
    }

    public void redrawErasedDrawingComponent(Vector<Integer> erasedComponentIds) {
        MyLog.i("drawing", "redraw erased component. ids=" + erasedComponentIds);
        for(int i=1; i<erasedComponentIds.size(); i++) {
            DrawingComponent component = findDrawingComponentById(erasedComponentIds.get(i));

            if(component == null)
                return;

            MyLog.i("drawing", "findComponentsToErase id=" + component.getId());

            component.setErased(true);
            component.drawComponent(getBackCanvas());
        }
    }

    public void clearDrawingBitmap() {
        drawingBitmap.eraseColor(Color.TRANSPARENT);
    }

    public void eraseDrawingBoardArray(Vector<Integer> erasedComponentIds) {
        for(int i=1; i<erasedComponentIds.size(); i++) {    //i=0 --> -1
            int id = erasedComponentIds.get(i);

            ArrayList<Point> newPoints = (drawingBoardMap.get(id));
            if(newPoints == null)
                return;

            MyLog.i("drawing", "id=" + id + ", newPoints.size()=" + newPoints.size());

            for(int j=0; j<newPoints.size(); j++) {
                int x = newPoints.get(j).x;
                int y = newPoints.get(j).y;

                if(drawingBoardArray[y][x].contains(id)) {
                    drawingBoardArray[y][x].removeElement(id);
                }
            }
            drawingBoardMap.remove(id);
        }
    }

    public void redraw(String usersComponentId) {
        if(lastDrawingBitmap == null) {
            drawingBitmap.eraseColor(Color.TRANSPARENT);
            return;
        }

        drawingBitmap = lastDrawingBitmap.copy(lastDrawingBitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);

        /*if(isDrawingShape) {
            for (DrawingComponent component : currentShapes) {
                try {
                    if (!component.getUsersComponentId().equals(usersComponentId))
                        component.drawComponent(backCanvas);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }*/
        /*Bitmap bitmap = history.get(history.size() - 1).getBitmap();
        //drawingBitmap = Bitmap.createBitmap(bitmap);
        drawingBitmap = bitmap.copy(bitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);*/
    }

    public void clearSelectedBitmap() {
        selectedComponentBitmap.eraseColor(Color.TRANSPARENT);
    }

    public void clearAllSelectedBitmap() {
        selectedComponentBitmap.eraseColor(Color.TRANSPARENT);
        preSelectedComponentsBitmap.eraseColor(Color.TRANSPARENT);
        postSelectedComponentsBitmap.eraseColor(Color.TRANSPARENT);
    }

    //fixme undo, redo
    public void undo() {
        if(history.size() == 0)
            return;

        undoArray.add(popHistory());

        if(history.size() == 0) {
            drawingBitmap.eraseColor(Color.TRANSPARENT);
            return;
        }

        MyLog.i("drawing", "history.size()=" + getHistory().size());
    }

    public void redo() {
        if(undoArray.size() == 0)
            return;

        history.add(popUndoArray());

        MyLog.i("drawing", "history.size()=" + getHistory().size());
    }

    public void clearDrawingComponents() {
        drawingBitmap.eraseColor(Color.TRANSPARENT);
        lastDrawingBitmap.eraseColor(Color.TRANSPARENT);
        undoArray.clear();
        history.clear();
        drawingComponents.clear();
        currentComponents.clear();
        componentId = -1;
        maxComponentId = -1;
        clearDrawingBoardArray();
        removedComponentId.clear();
        drawingBoardMap.clear();
    }

    public void clearTexts() {
        removeAllTextViewToFrameLayout();
        getTexts().clear();
        textId = -1;
    }

    public void clearBackgroundImage() {
        drawingFragment.getBinding().backgroundView.removeAllViews();
    }

    public byte[] bitmapToByteArray(Bitmap bitmap){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        //bitmap.recycle();
        return stream.toByteArray();
    }

    public Bitmap byteArrayToBitmap(byte[] byteArray) {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }


    //-----setter-----

    public void setDrawingFragment(DrawingFragment drawingFragment) {
        this.drawingFragment = drawingFragment;
    }

    public void setTextMoveBorderDrawable(Drawable textMoveBorderDrawable) { this.textMoveBorderDrawable = textMoveBorderDrawable; } // 텍스트 테두리 그리기 위한 Drawable 설정

    public void setTextFocusBorderDrawable(Drawable textFocusBorderDrawable) { this.textFocusBorderDrawable = textFocusBorderDrawable; }

    public void setTextHighlightBorderDrawable(Drawable textHighlightBorderDrawble) { this.textHighlightBorderDrawble = textHighlightBorderDrawble; } // fixme nayeon

    public void setBackgroundImage(Bitmap backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public void setDrawingBitmap(Bitmap drawingBitmap) {
        this.drawingBitmap = drawingBitmap;
    }

    public void setBackCanvas(Canvas backCanvas) {
        this.backCanvas = backCanvas;
    }

    public void setLastDrawingBitmap(Bitmap lastDrawingBitmap) {
        this.lastDrawingBitmap = lastDrawingBitmap;
    }

    public void setCurrentText(Text text) { this.currentText = text; }

    public void setTextBeingEdited(Boolean bool) { this.isTextBeingEdited = bool; } // 하나의 텍스트 편집 시 다른 텍스트 포커싱 막기 위해

    public void setMidEntered(Boolean bool) { this.isMidEntered = bool; } // fixme nayeon

    public void setHistory(ArrayList<DrawingItem> history) {
        this.history = history;
    }

    public void setUndoArray(ArrayList<DrawingItem> undoArray) {
        this.undoArray = undoArray;
    }

    public void setRemovedComponentId(Vector<Integer> removedComponentId) {
        this.removedComponentId = removedComponentId;
    }

    public void setCurrentMode(Mode currentMode) {
        this.currentMode = currentMode;
    }

    public void setCurrentType(ComponentType currentType) {
        this.currentType = currentType;
    }

    public void setDrawnCanvasWidth(float drawnCanvasWidth) {
        this.drawnCanvasWidth = drawnCanvasWidth;
    }

    public void setDrawnCanvasHeight(float drawnCanvasHeight) {
        this.drawnCanvasHeight = drawnCanvasHeight;
    }

    public void setMyCanvasWidth(float myCanvasWidth) {
        this.myCanvasWidth = myCanvasWidth;
    }

    public void setMyCanvasHeight(float myCanvasHeight) {
        this.myCanvasHeight = myCanvasHeight;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setMyUsername(String myUsername) {
        this.myUsername = myUsername;
    }

    public void setFillColor(String fillColor) {
        this.fillColor = fillColor;
    }

    public void setStrokeColor(String strokeColor) {
        this.strokeColor = strokeColor;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public void setFontStyle(int fontStyle) {
        this.fontStyle = fontStyle;
    }

    public void setTextBackground(int textBackground) {
        this.textBackground = textBackground;
    }

    //public void setTextIdInCallback(int myTextArrayIndex) { this.texts.get(myTextArrayIndex).getTextAttribute().setId(this.textId); }

    public void setDrawingComponents(ArrayList<DrawingComponent> drawingComponents) { this.drawingComponents = drawingComponents; }

    public void setTexts(ArrayList<Text> texts) { this.texts = texts; }

    public void setMaxComponentId(int maxComponentId) {
        this.maxComponentId = maxComponentId;
    }

    public void setTextId(int textId) { this.textId = textId; }

    public void setIntercept(boolean intercept) {
        isIntercept = intercept;
        MyLog.i("intercept", intercept + "");
    }

    public void setSelectedComponent(DrawingComponent selectedComponent) {
        this.selectedComponent = selectedComponent;
    }

    public void setSelectedComponentBitmap(Bitmap selectedComponentBitmap) {
        this.selectedComponentBitmap = selectedComponentBitmap;
    }

    public void setPreSelectedComponentsBitmap(Bitmap preSelectedComponentsBitmap) {
        this.preSelectedComponentsBitmap = preSelectedComponentsBitmap;
    }

    public void setPostSelectedComponentsBitmap(Bitmap postSelectedComponentsBitmap) {
        this.postSelectedComponentsBitmap = postSelectedComponentsBitmap;
    }

    /*public void setCurrentDrawAction(int currentDrawAction) {
        this.currentDrawAction = currentDrawAction;
    }*/

    public void setDrawingShape(boolean drawingShape) {
        isDrawingShape = drawingShape;
    }

}
