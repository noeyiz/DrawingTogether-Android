package com.hansung.drawingtogether.view.drawing;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.util.SparseArray;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import lombok.Getter;

@Getter
public enum DrawingEditor {
    INSTANCE;

    private DrawingFragment drawingFragment;
    private DrawingView drawingView;

    private Bitmap backgroundImage;             //
    private Bitmap drawingBitmap;               //그리기 bitmap
    private Canvas backCanvas;                  //미리 그려두기 위한 Canvas
    private Bitmap lastDrawingBitmap = null;    //drawingBitmap 의 마지막 상태 bitmap --> 도형 그리기

    private int componentId = -1;
    private Vector<Integer> removedComponentId = new Vector<>();
    private Vector<Integer>[][] drawingBoardArray;
    private SparseArray<ArrayList<Point>> drawingBoardMap = new SparseArray<>();

    private ArrayList<DrawingComponent> drawingComponents = new ArrayList<>();  //현재 그려져있는 모든 drawing component 배열
    private ArrayList<DrawingComponent> currentComponents = new ArrayList<>();  //현재 그리기중인 drawing component 배열

    private ArrayList<Text> texts = new ArrayList<>(); // 현재 부착된 모든 text 배열
    private Text currentText = null;
    private boolean isTextBeingEdited = false;
    private int textId = -1;

    private ArrayList<DrawingItem> history = new ArrayList<>();     //
    private ArrayList<DrawingItem> undoArray = new ArrayList<>();   //undo 배열

    private Mode currentMode = Mode.DRAW;
    private ComponentType currentType = ComponentType.STROKE;
    private String myUsername;
    private String username;
    private float drawnCanvasWidth;
    private float drawnCanvasHeight;
    private float myCanvasWidth;
    private float myCanvasHeight;
    private int fillColor = Color.TRANSPARENT;  //fixme
    private int strokeColor = Color.BLACK;      //fixme
    private int strokeAlpha = 255;              //fixme
    private int fillAlpha = 100;                //fixme
    private int strokeWidth = 50;              //fixme


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

        texts.clear();
        currentText = null;
        isTextBeingEdited = false;
        textId = -1;

        history.clear();
        undoArray.clear();

        currentMode = Mode.DRAW;
        currentType = ComponentType.STROKE;

        removeAllTextViewToFrameLayout();
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
        Iterator<DrawingComponent> iterator = drawingComponents.iterator();
        while(iterator.hasNext()) {
            iterator.next().drawComponent(getBackCanvas());
        }
    }

    public void drawAllDrawingComponentsForMid(float myCanvasWidth, float myCanvasHeight) {   //drawingComponents draw
        Iterator<DrawingComponent> iterator = drawingComponents.iterator();
        while(iterator.hasNext()) {
            DrawingComponent component = iterator.next();
            component.calculateRatio(myCanvasWidth, myCanvasHeight);
            component.drawComponent(getBackCanvas());

            splitPoints(component, myCanvasWidth, myCanvasHeight);
        }
        Log.i("drawing", "myCanvas w=" + myCanvasWidth + ", h=" + myCanvasHeight);
        Log.i("drawing", "drawingBoardArray[][] w=" + drawingBoardArray[0].length + ", h=" + drawingBoardArray.length);
        Log.i("drawing", "dba[0][0] = " + drawingBoardArray[0][0].get(0));
    }

    /*public void drawCurrentComponents() {   //currentComponents draw
        for(DrawingComponent component: currentComponents) {
            component.draw(getBackCanvas());
        }
    }*/

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
        //Vector<DrawingComponent> components = new Vector<>();
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

    /*public Vector<DrawingComponent> findCurrentComponents(String username) {    //username이 다른 drawing component vector return
        Vector<DrawingComponent> components = new Vector<>();
        for(DrawingComponent component: currentComponents) {
            if(!component.getUsername().equals(username))
                components.add(component);
        }
        return components;
    }*/

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

    /*public void removeAllDrawingComponents(Vector<DrawingComponent> components) {
        drawingComponents.removeAll(components);
    }*/

    public void removeAllDrawingComponents(Vector<Integer> ids) {
        for(int i: ids)
            removeDrawingComponents(i);
    }

    public void removeDrawingComponents(int id) {
        for(DrawingComponent component: drawingComponents) {
            if(component.getId() == id) {
                drawingComponents.remove(component);
                break;
            }
        }
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

    /*public void updateDrawingComponentsId(int changedId, DrawingComponent component) {
        removeDrawingComponents(component.getId());

        component.setId(changedId);
        drawingComponents.add(component);

        for(DrawingComponent dc: drawingComponents) {
            Log.i("drawing", dc.getId() + "");
        }
    }*/

    public void updateDrawingComponents(Vector<DrawingComponent> components) {
        //속성 변경 update
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

    //fixme nayeon - 텍스트 동시
    public String setTextStringId() { return myUsername + "-" + textIdCounter(); }

    public void removeAllTextViewToFrameLayout() {
        for(Text t: texts) {
            t.removeTextViewToFrameLayout();
        }
    }

    /*public void addAllTextViewToFrameLayout() {
        for(Text t: texts) {
            t.addTextViewToFrameLayout();
        }
    }*/

    public void addAllTextViewToFrameLayoutForMid() {
        for(Text t: texts) {
            t.addTextViewToFrameLayout();
            t.createGestureDetecter(); // 텍스트 모두 붙이기를 중간자 처리, 재접속 시에만 한다고 가정했을 때. // fixme nayeon
        }
    }

    public int textIdCounter() {
        return ++textId;
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

    public void updateDrawingComponents(DrawingItem lastItem) {
        if(lastItem.getComponents() == null)
            return;

        Log.i("drawing", "mode = " + lastItem.getMode().toString());

        Vector<Integer> ids = new Vector<>();
        for (DrawingComponent component: lastItem.getComponents()) {
            ids.add(component.getId());
        }
        Log.i("drawing", "last item ids = " + ids.toString());

        String str = "dc = ";
        for(DrawingComponent component: getDrawingComponents()) {
            str += component.getId() + " ";
        }
        Log.i("drawing", str);

        switch(lastItem.getMode()) {
            case DRAW:
            case ERASE:
                //if(drawingComponents.containsAll(lastItem.getComponents())) {       //erase
                if(isContainsAllDrawingComponents(ids)) {       //erase
                    Log.i("drawing", "update erase");
                    clearDrawingBitmap();
                    addRemovedComponentIds(ids);
                    //removeAllDrawingComponents(lastItem.getComponents());
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
                    //drawAllDrawingComponents();
                }
                setLastDrawingBitmap(getDrawingBitmap().copy(getDrawingBitmap().getConfig(), true));
                Log.i("drawing", "removedComponentIds = " + getRemovedComponentId());

                break;

            case SELECT:
            case GROUP:
                break;
        }
    }

    public void updateTexts(DrawingItem lastItem, boolean isUndo) {
        if(lastItem.getTextAttribute() == null)
            return;

        Log.i("drawing", "text mode = " + lastItem.getTextMode().toString());

        TextAttribute textAttr = lastItem.getTextAttribute();
        Text text = findTextById(textAttr.getId());

        switch(lastItem.getTextMode()) {
            case CREATE:
            case ERASE:
                if(text != null && texts.contains(text)) {              //erase
                    text.setTextAttribute(textAttr);
                    text.removeTextViewToFrameLayout();
                    removeTexts(text);
                    Log.i("drawing", "texts.size()=" + texts.size());
                } else {
                    Text newText = new Text(drawingFragment, textAttr); //create
                    newText.getTextAttribute().setTextInited(true);
                    newText.addTextViewToFrameLayout();
                    newText.createGestureDetecter();
                    Log.i("drawing", "texts.size()=" + texts.size());
                }
                break;

            case MODIFY:
                if(isUndo) {
                    Log.i("drawing", "pre text = " + textAttr.getPreText() );
                    textAttr.setPostText(textAttr.getText());
                    textAttr.setText(textAttr.getPreText());
                } else {
                    Log.i("drawing", "post text = " + textAttr.getPostText() );
                    textAttr.setPreText(textAttr.getText());
                    textAttr.setText(textAttr.getPostText());
                }
                if(text != null)
                    text.modifyTextViewContent(textAttr.getText());

                break;

            case DROP:
                if(text != null) {
                    if(isUndo)
                        text.setPreTextViewLocation();
                    else
                        text.setTextViewLocation();
                }
                break;
        }
        //invalidateDrawingView();
    }

    public DrawingItem popHistory() {   //undo
        int index = history.size() - 1;
        DrawingItem lastItem = history.get(index);
        history.remove(index);

        if(lastItem.getMode() != null)
            updateDrawingComponents(lastItem);
        else if(lastItem.getTextMode() != null) {
            updateTexts(lastItem, true);
        }
        return lastItem;
    }

    public DrawingItem popUndoArray() {  //redo
        int index = undoArray.size() - 1;
        DrawingItem lastItem = undoArray.get(index);

        if (lastItem.getMode() != null)
            updateDrawingComponents(lastItem);
        else if (lastItem.getTextMode() != null) {
            updateTexts(lastItem, false);
        }
        undoArray.remove(index);
        return lastItem;
    }

    public void clearUndoArray() {
        undoArray.clear();
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
        return ++componentId;

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
                    drawingBoardArray[i][j].clear();
                    drawingBoardArray[i][j].add(-1);
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


    /*

    //겹쳐진 부분만 찾아서 지우기

    public void redrawUnErasedDrawingComponent() {
        Collections.sort(redrawIds);
        for(int i=0; i<redrawIds.size(); i++) {
            DrawingComponent component = findDrawingComponentById(redrawIds.get(i));

            if(component == null)
                return;

            component.drawComponent(getBackCanvas());
        }

        redrawIds.clear();
    }

    public void findUnErasedDrawingComponent(int id) {
        //DrawingComponent component = findDrawingComponentById(id);
        ArrayList<Point> newPoints = drawingBoardMap.get(id);

        Log.i("drawing", "redraw id=" + id);

        //component.drawComponent(getBackCanvas());

        //Vector<Integer> redrawIds = new Vector<>();
        for(int i=0; i<newPoints.size(); i++) {
            int x = newPoints.get(i).x;
            int y = newPoints.get(i).y;

            if(drawingBoardArray[y][x].contains(id)) {
                if(drawingBoardArray[y][x].get(drawingBoardArray[y][x].size() - 1) != id) {
                    if(!redrawIds.contains(drawingBoardArray[y][x].get(drawingBoardArray[y][x].size() - 1))) {
                        redrawIds.add(drawingBoardArray[y][x].get(drawingBoardArray[y][x].size() - 1));
                        findUnErasedDrawingComponent(drawingBoardArray[y][x].get(drawingBoardArray[y][x].size() - 1));
                        Log.i("drawing", "x=" + x + ", y=" + y);
                    }

                }
            }
        }
    }

    Vector<Integer> redrawIds = new Vector<>();
    public void eraseDrawingComponents(Vector<Integer> erasedComponentIds) {
        //redrawErasedDrawingComponent(erasedComponentIds);

        //Vector<Integer> redrawIds = new Vector<>();
        for(int i=1; i<erasedComponentIds.size(); i++) {    //i=0 --> -1
            int id = erasedComponentIds.get(i);

            ArrayList<Point> newPoints = (drawingBoardMap.get(id));
            Log.i("drawing", "id=" + id + ", newPoints.size()=" + newPoints.size());

            for(int j=0; j<newPoints.size(); j++) {
                int x = newPoints.get(j).x;
                int y = newPoints.get(j).y;

                if(drawingBoardArray[y][x].contains(id)) {
                    drawingBoardArray[y][x].removeElement(id);
                    if(drawingBoardArray[y][x].size() > 1) {
                        //redrawUnErasedDrawingComponent(drawingBoardArray[y][x].get(drawingBoardArray[y][x].size() - 1));
                        if(!redrawIds.contains(drawingBoardArray[y][x].get(drawingBoardArray[y][x].size() - 1)))
                            redrawIds.add(drawingBoardArray[y][x].get(drawingBoardArray[y][x].size() - 1));

                    }
                }
            }
            removeDrawingComponents(id);
            drawingBoardMap.remove(id);
        }
        Collections.sort(redrawIds);
        Log.i("drawing", "redraw unErased ids = " + redrawIds.toString());
        for(int i=0; i<redrawIds.size(); i++) {
            findUnErasedDrawingComponent(redrawIds.get(i));
        }
        redrawUnErasedDrawingComponent();

        addHistory(new DrawingItem(this.getCurrentMode(), erasedComponentIds, (drawingBitmap)));    //fixme
        //drawingBitmap.eraseColor(Color.TRANSPARENT);
        //drawAllDrawingComponents();
    }

     */


    public void eraseDrawingBoardArray(Vector<Integer> erasedComponentIds) {
        /*for(int i=1;i<erasedComponentIds.size(); i++) {
            int id = erasedComponentIds.get(i);
            removeDrawingComponents(id);
            drawAllDrawingComponents();
        }*/

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
            //removeDrawingComponents(id);
            drawingBoardMap.remove(id);
        }

        //drawAllDrawingComponents();
        //addHistory(new DrawingItem(this.getCurrentMode(), erasedComponentIds, (drawingBitmap)));    //fixme
    }

    public void redraw() {
        //getDrawingBitmap().eraseColor(Color.TRANSPARENT);
        //drawAllDrawingComponents();

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
        /*Bitmap bitmap = history.get(history.size() - 1).getBitmap();
        drawingBitmap = bitmap.copy(bitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);*/
    }

    public void redo() {
        if(undoArray.size() == 0)
            return;

        history.add(popUndoArray());

        Log.i("drawing", "history.size()=" + getHistory().size());
        /*Bitmap bitmap = history.get(history.size() - 1).getBitmap();
        drawingBitmap = bitmap.copy(bitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);*/
    }

    public void clearDrawingComponents() {
        drawingBitmap.eraseColor(Color.TRANSPARENT);
        lastDrawingBitmap.eraseColor(Color.TRANSPARENT);
        undoArray.clear();
        history.clear();
        drawingComponents.clear();
        componentId = -1;
        clearDrawingBoardArray();
        drawingBoardMap.clear();
    }

    public void clearTexts() {
        removeAllTextViewToFrameLayout();
        getTexts().clear();
        textId = -1;
    }

    public void invalidateDrawingView() {
        setDrawingView(getDrawingFragment().getBinding().drawingView);
        drawingView.invalidate();
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

    public void setDrawingView(DrawingView drawingView) {
        this.drawingView = drawingView;
    }

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

    public void setDrawingBoardArray(Vector<Integer>[][] drawingBoardArray) {
        this.drawingBoardArray = drawingBoardArray;
    }

    public void setCurrentText(Text text) { this.currentText = text; }

    public void setTextBeingEdited(Boolean bool) { this.isTextBeingEdited = bool; } // fixme nayeon

    public void setHistory(ArrayList<DrawingItem> history) {
        this.history = history;
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

    //public void setTextIdInCallback(int myTextArrayIndex) { this.texts.get(myTextArrayIndex).getTextAttribute().setId(this.textId); }

    public void setDrawingComponents(ArrayList<DrawingComponent> drawingComponents) {
        this.drawingComponents = drawingComponents;
    }

    public void setTexts(ArrayList<Text> texts) { this.texts = texts; }
    // public void setBackgroundImage(Bitmap backgroundImage) { this.backgroundImage = backgroundImage; } // 위에 선언되어있음

    public void setComponentId(int componentId) { this.componentId = componentId; }

    public void setTextId(int textId) { this.textId = textId; }
}
