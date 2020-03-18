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
import android.util.Log;
import android.util.SparseArray;
import android.widget.EditText;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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
    private int componentId = -1;
    private int maxComponentId = -1;
    private Vector<Integer> removedComponentId = new Vector<>();
    private Vector<Integer>[][] drawingBoardArray = null;
    private SparseArray<ArrayList<Point>> drawingBoardMap = new SparseArray<>();

    private ArrayList<DrawingComponent> drawingComponents = new ArrayList<>();  //현재 그려져있는 모든 drawing component 배열
    private ArrayList<DrawingComponent> currentComponents = new ArrayList<>();  //현재 그리기중인 drawing component 배열

    /* selector 에 필요한 객체 */
    private DrawingComponent selectedComponent;
    private Vector<DrawingComponent> preSelectedComponents = new Vector<>();
    private Vector<DrawingComponent> postSelectedComponents = new Vector<>();
    private Bitmap selectedComponentBitmap;
    private Bitmap preSelectedComponentsBitmap;
    private Bitmap postSelectedComponentsBitmap;

    /* 텍스트에 필요한 객체 */
    private Drawable textMoveBorderDrawable; // 텍스트 포커싱 테두리
    private Drawable textFocusBorderDrawable;
    private ArrayList<Text> texts = new ArrayList<>(); // 현재 부착된 모든 text 배열
    private Text currentText = null;
    private boolean isTextBeingEdited = false;
    private boolean isTextBeingModified = false; // fixme nayeon
    private int textId = -1;
    private int maxTextId = -1;

    /* UNDO, REDO 를 위한 객체 */
    private ArrayList<DrawingItem> history = new ArrayList<>();     //
    private ArrayList<DrawingItem> undoArray = new ArrayList<>();   //undo 배열

    /* 드로잉 컴포넌트 속성 */
    private Mode currentMode = Mode.DRAW;
    private ComponentType currentType = ComponentType.STROKE;
    private String myUsername;
    private String username;
    private float drawnCanvasWidth;
    private float drawnCanvasHeight;
    private float myCanvasWidth;
    private float myCanvasHeight;

    /* 드로잉 펜 속성 */
    private int fillColor = Color.TRANSPARENT;  // todo nayeon - Default Value
    private int strokeColor = Color.BLACK;
    private int strokeAlpha = 255;
    private int fillAlpha = 100;
    private int strokeWidth = 10;

    /* 텍스트 속성 */
    private int textSize = 20;
    private int textColor = Color.BLACK;
    private int fontStyle = Typeface.BOLD;
    private int textBackground = Color.TRANSPARENT;

    // 드로잉 하는동안 저장되는 모든 데이터들 지우기 [나가기 버튼 눌렀을 때 처리 필요 - MQTTClient.java if(topic_exit, topic_delete) 부분에서 호출]
    public void removeAllDrawingData() {
        backgroundImage = null;
        drawingBitmap = null;
        lastDrawingBitmap = null;

        componentId = -1;
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
        strokeColor = Color.BLACK;
        strokeWidth = 10;

        isIntercept = false;
    }

    public void printDrawingData() {
        Log.i("backgroundImage", backgroundImage.toString());
        Log.i("drawingBitmap", drawingBitmap.toString());
        Log.i("lastDrawingBitmap", lastDrawingBitmap.toString());

        Log.i("componentId", Integer.toString(componentId));
        Log.i("removedComponentId", Integer.toString(removedComponentId.size()));
        Log.i("drawingBoardArray", Integer.toString(drawingBoardArray.length));
        Log.i("drawingBoardMap", Integer.toString(drawingBoardArray.length));

        Log.i("drawingComponents", Integer.toString(drawingComponents.size()));
        Log.i("currentComponents", Integer.toString(currentComponents.size()));

        Log.i("texts", Integer.toString(texts.size()));
        Log.i("currentText", currentText.toString());
        Log.i("isTextBeingEdited", Boolean.toString(isTextBeingEdited));

        Log.i("history", Integer.toString(history.size()));
        Log.i("undoArray", Integer.toString(undoArray.size()));
    }

    public static DrawingEditor getInstance() { return INSTANCE; }
    /*public DrawingEditor() {  }
    public static DrawingEditor getInstance() { return LazyHolder.INSTANCE; }
    private static class LazyHolder {
        private static final DrawingEditor INSTANCE = new DrawingEditor();
    }*/

    public void drawAllDrawingComponents() {   //drawingComponents draw
        for (DrawingComponent drawingComponent : drawingComponents) {
            drawingComponent.drawComponent(getBackCanvas());
        }
    }

    public void drawAllDrawingComponentsForMid() {   //drawingComponents draw
        for (DrawingComponent component : drawingComponents) {

            component.calculateRatio(drawingBoardArray[0].length, drawingBoardArray.length);
            component.drawComponent(getBackCanvas());

            splitPoints(component, drawingBoardArray[0].length, drawingBoardArray.length);
        }

        Log.i("drawing", "drawingBoardArray[][] w=" + drawingBoardArray[0].length + ", h=" + drawingBoardArray.length);
        Log.i("drawing", "dba[0][0] = " + drawingBoardArray[0][0].get(0));
    }

    public void addCurrentComponents(DrawingComponent component) {
        this.currentComponents.add(component);
    }

    public void removeCurrentComponents(int id) {
        for(DrawingComponent component: currentComponents) {
            if(component.getId() == id) {
                currentComponents.remove(component);
                break;
            }
        }
    }

    public boolean isContainsCurrentComponents(int id) {    //다른 디바이스에서 동시에 그렸을 경우
        String str = "cc = ";
        for(DrawingComponent component: getCurrentComponents()) {
            str += component.getId() + " ";
        }
        Log.i("drawing", str);

        for(DrawingComponent component: currentComponents) {
            if(component.getId() == id)
                return true;
        }
        return false;
    }

    public DrawingComponent findCurrentComponent(String usersComponentId) {
        for(DrawingComponent component: currentComponents) {
            if(component.getUsersComponentId().equals(usersComponentId))
                return component;
        }
        return null;
    }

    public void addDrawingComponents(DrawingComponent component) {
        this.drawingComponents.add(component);
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
                drawingComponents.remove(drawingComponents.get(i));
                return i;
            }
        }
        return -1;
    }

    public boolean isContainsAllDrawingComponents(Vector<Integer> ids) {
        for(int i: ids) {
            if(!isContainsDrawingComponents(i))
                return false;
        }
        return true;
    }

    public boolean isContainsDrawingComponents(int id) {
        for(DrawingComponent component: drawingComponents) {
            if(component.getId() == id)
                return true;
        }
        return false;
    }

    public void updateDrawingComponents(DrawingComponent newComponent) {    //속성 변경 update
        int index = removeDrawingComponents(newComponent.getId());
        drawingComponents.add(index, newComponent);
        //addDrawingComponents(newComponent);
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
        Log.i("drawing", str);
    }

    public void setPostSelectedComponents(int id) {
        postSelectedComponents.clear();
        for(DrawingComponent component: drawingComponents) {
            if(id < component.getId())
                postSelectedComponents.add(component);
        }
        String str = "post selected = ";
        for(DrawingComponent component: postSelectedComponents) {
            str += component.getId() + " ";
        }
        Log.i("drawing", str);
    }

    public boolean isContainsSelectedComponent(Point point) {
        try {
            if (selectedComponent.getType() == ComponentType.STROKE) return false;

            Point datumPoint = selectedComponent.getDatumPoint();
            int width = selectedComponent.getWidth();
            int height = selectedComponent.getHeight();
            if ((datumPoint.x <= point.x && point.x <= datumPoint.x + width) && (datumPoint.y <= point.y && point.y <= datumPoint.y + height))
                return true;

        } catch(NullPointerException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void drawSelectedComponentBorder(DrawingComponent component, float canvasWidth, float canvasHeight) {
        component.calculateRatio(canvasWidth, canvasHeight);
        Point datumPoint = component.getDatumPoint();
        int width = component.getWidth();
        int height = component.getHeight();
        int strokeWidth = component.getStrokeWidth();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        try {
            DashPathEffect dashPath = new DashPathEffect(new float[]{12, 4}, 4);
            paint.setPathEffect(dashPath);
            paint.setStrokeWidth(4);
            //paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);     //윤곽선
            paint.setColor(Color.LTGRAY);
            getSelectedCanvas().drawRect(datumPoint.x - strokeWidth/2 - 10, datumPoint.y - strokeWidth/2 - 10, datumPoint.x + width + strokeWidth/2 + 10, datumPoint.y + height + strokeWidth/2 + 10, paint);
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
        postSelectedComponentsBitmap.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(postSelectedComponentsBitmap);
        for(DrawingComponent component: postSelectedComponents) {
            component.drawComponent(canvas);
        }
    }

    public void drawSelectedBitmaps() {
        drawingBitmap.eraseColor(Color.TRANSPARENT);
        drawingBitmap = preSelectedComponentsBitmap.copy(preSelectedComponentsBitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);
        backCanvas.drawBitmap(postSelectedComponentsBitmap, 0, 0, null);
    }

    public void moveSelectedComponent(int moveX, int moveY) {
        selectedComponent.setBeginPoint(new Point(selectedComponent.getBeginPoint().x + moveX, selectedComponent.getBeginPoint().y + moveY));
        selectedComponent.setEndPoint(new Point(selectedComponent.getEndPoint().x + moveX, selectedComponent.getEndPoint().y + moveY));
        selectedComponent.setDatumPoint(new Point(selectedComponent.getDatumPoint().x + moveX, selectedComponent.getDatumPoint().y + moveY));
    }

    public void updateDrawingBitmap() {
        drawingBitmap.eraseColor(Color.TRANSPARENT);
        drawingBitmap = preSelectedComponentsBitmap.copy(preSelectedComponentsBitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);
        clearSelectedBitmap();
        selectedComponent.drawComponent(backCanvas);
        drawSelectedComponentBorder(getSelectedComponent(), getMyCanvasWidth(), getMyCanvasHeight()); //
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

            Point datumPoint = component.getDatumPoint();
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

            // fixme nayeon
            t.setTextViewInitialPlace(t.getTextAttribute());
            t.setTextViewProperties();

            t.addTextViewToFrameLayout();
            t.createGestureDetecter(); // 텍스트 모두 붙이기를 중간자 처리, 재접속 시에만 한다고 가정했을 때.

            Log.e("texts size, text id", texts.size() + ", " + t.getTextAttribute().getId());
            Log.e("text view size", t.getTextView().getWidth() + ", " + t.getTextView().getHeight());
            Log.e("text view location", t.getTextView().getX() + ", " + t.getTextView().getY());
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

    public void updateDrawingComponents(DrawingItem lastItem) {
        if(lastItem.getComponents() == null)
            return;

        Log.i("drawing", "mode = " + lastItem.getMode().toString());

        Vector<Integer> ids = new Vector<>();
        for (DrawingComponent component: lastItem.getComponents()) {
            ids.add(component.getId());
        }
        Log.i("drawing", "last item ids = " + ids.toString());

        StringBuilder str = new StringBuilder("dc = ");
        for(DrawingComponent component: getDrawingComponents()) {
            str.append(component.getId()).append(" ");
        }
        Log.i("drawing", str.toString());

        switch(lastItem.getMode()) {
            case DRAW:
            case ERASE:
                if(isContainsAllDrawingComponents(ids)) {                           //erase
                    Log.i("drawing", "update erase");
                    clearDrawingBitmap();
                    addRemovedComponentIds(ids);
                    removeAllDrawingComponents(ids);
                    drawAllDrawingComponents();
                    eraseDrawingBoardArray(ids);
                } else {
                    Log.i("drawing", "update draw");
                    for (DrawingComponent component: lastItem.getComponents()) {    //draw
                        component.calculateRatio(myCanvasWidth, myCanvasHeight);
                        component.drawComponent(getBackCanvas());
                        splitPoints(component, myCanvasWidth, myCanvasHeight);
                        //component.setIsErased(false);
                    }
                    removeRemovedComponentIds(ids);
                    addAllDrawingComponents(lastItem.getComponents());
                }
                setLastDrawingBitmap(getDrawingBitmap().copy(getDrawingBitmap().getConfig(), true));
                Log.i("drawing", "removedComponentIds = " + getRemovedComponentId());

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
            updateDrawingComponents(lastItem);
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

    public int componentIdCounter() {
        maxComponentId = ++componentId;
        return maxComponentId;
    }

    public void initDrawingBoardArray(int width, int height) {
        Log.i("drawing", "initDrawingBoardArray()");
        try{
            drawingBoardArray = new Vector[height][width];

            for(int i=0; i<height; i++) {
                for(int j=0; j<width; j++) {
                    drawingBoardArray[i][j] = new Vector<>();
                    drawingBoardArray[i][j].add(-1);
                }
            }
        } catch(OutOfMemoryError e) {
            Log.i("mqtt", "Out of Memory Error at initDrawingBoardArray()");
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
            Log.i("mqtt", "Out of Memory Error at clearDrawingBoardArray()");
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
        Log.i("drawing", "calcPoints = " + calcPoints.toString());

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
        Log.i("drawing", "calcBegin = " + calcBeginPoint.toString() + ", calcEnd = " + calcEndPoint.toString());

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

        component.setDatumPoint(datumPoint);
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
                switch (component.getType()) {
                    case STROKE:
                        break;

                    case RECT:
                    case OVAL:
                        Point datumPoint = component.getDatumPoint();
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

    public void redrawErasedDrawingComponent(Vector<Integer> erasedComponentIds) {
        Log.i("drawing", "redraw erased component. ids=" + erasedComponentIds);
        for(int i=1; i<erasedComponentIds.size(); i++) {
            DrawingComponent component = findDrawingComponentById(erasedComponentIds.get(i));

            if(component == null)
                return;

            Log.i("drawing", "findComponentsToErase id=" + component.getId());

            component.setIsErased(true);
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

            Log.i("drawing", "id=" + id + ", newPoints.size()=" + newPoints.size());

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

    public void redraw() {
        if(lastDrawingBitmap == null) {
            drawingBitmap.eraseColor(Color.TRANSPARENT);
            return;
        }

        drawingBitmap = lastDrawingBitmap.copy(lastDrawingBitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);

        /*Bitmap bitmap = history.get(history.size() - 1).getBitmap();
        //drawingBitmap = Bitmap.createBitmap(bitmap);
        drawingBitmap = bitmap.copy(bitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);*/
    }

    public void clearSelectedBitmap() {
        selectedComponentBitmap.eraseColor(Color.TRANSPARENT);
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

        Log.i("drawing", "history.size()=" + getHistory().size());
    }

    public void redo() {
        if(undoArray.size() == 0)
            return;

        history.add(popUndoArray());

        Log.i("drawing", "history.size()=" + getHistory().size());
    }

    public void clearDrawingComponents() {
        drawingBitmap.eraseColor(Color.TRANSPARENT);
        lastDrawingBitmap.eraseColor(Color.TRANSPARENT);
        undoArray.clear();
        history.clear();
        drawingComponents.clear();
        currentComponents.clear();
        componentId = -1;
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
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
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

    public void setTextBeingModified(Boolean bool) { this.isTextBeingModified = bool; } // 텍스트 편집 시작 시점 알기위해

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

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
    }

    public void setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public void setTextColor(int textColor) {
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

    public void setComponentId(int componentId) { this.componentId = componentId; }

    public void setTextId(int textId) { this.textId = textId; }

    public void setIntercept(boolean intercept) {
        isIntercept = intercept;
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
}
