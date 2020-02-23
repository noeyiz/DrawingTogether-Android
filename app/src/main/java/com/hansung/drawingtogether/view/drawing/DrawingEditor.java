package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

    private Context context;
    private DrawingView drawingView;

    private Bitmap backgroundImage;             //
    private Bitmap drawingBitmap;               //그리기 bitmap
    private Canvas backCanvas;                  //미리 그려두기 위한 Canvas
    private Bitmap lastDrawingBitmap;           //drawingBitmap 의 마지막 상태 bitmap --> 도형 그리기

    private int componentId = -1;
    private Vector<Integer> removedComponentId = new Vector<>();
    private Vector<Integer>[][] drawingBoardArray;
    private SparseArray<ArrayList<Point>> drawingBoardMap = new SparseArray<>();

    private ArrayList<DrawingComponent> drawingComponents = new ArrayList<>();  //현재 그려져있는 모든 drawing component 배열
    private ArrayList<DrawingComponent> currentComponents = new ArrayList<>();  //현재 그리기중인 drawing component 배열

    private ArrayList<Text> texts = new ArrayList<>(); // 현재 부착된 모든 text 배열
    private Text currentText = null;
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
    private int strokeWidth = 100;              //fixme



    public static DrawingEditor getInstance() { return INSTANCE; }
    /*public DrawingEditor() {  }
    public static DrawingEditor getInstance() { return LazyHolder.INSTANCE; }
    private static class LazyHolder {
        private static final DrawingEditor INSTANCE = new DrawingEditor();
    }*/

    public void drawAllComponents() {   //drawingComponents draw
        Iterator<DrawingComponent> iterator = drawingComponents.iterator();
        while(iterator.hasNext()) {
            iterator.next().drawComponent(getBackCanvas());
        }
    }

    /*public void drawCurrentComponents() {   //currentComponents draw
        for(DrawingComponent component: currentComponents) {
            component.draw(getBackCanvas());
        }
    }*/

    public void addCurrentComponents(DrawingComponent component) {
        this.currentComponents.add(component);
    }

    public void removeCurrentComponents(DrawingComponent component) {
        currentComponents.remove(component);
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
        for(DrawingComponent component: currentComponents) {
            str += component.getId() + " ";
        }
        Log.i("drawing", str);

        for(DrawingComponent component: currentComponents) {
            if(component.getId() == id)
                return true;
        }
        return false;
    }

    public Vector<DrawingComponent> findCurrentComponents(String username) {    //username이 다른 drawing component vector return
        Vector<DrawingComponent> components = new Vector<>();
        for(DrawingComponent component: currentComponents) {
            if(!component.getUsername().equals(username))
                components.add(component);
        }
        return components;
    }

    public void addDrawingComponents(DrawingComponent component) {
        this.drawingComponents.add(component);
    }

    public void addAllDrawingComponents(Vector<DrawingComponent> components) {
        this.drawingComponents.addAll(components);
    }

    public void removeDrawingComponents(DrawingComponent component) {
        drawingComponents.remove(component);
    }

    public void removeAllDrawingComponents(Vector<DrawingComponent> components) {
        drawingComponents.removeAll(components);
    }

    public void removeDrawingComponents(int id) {
        for(DrawingComponent component: drawingComponents) {
            if(component.getId() == id) {
                drawingComponents.remove(component);
                break;
            }
        }
    }

    public boolean isContainsDrawingComponents(int id) {    //다른 디바이스에서 동시에 그렸을 경우
        //Vector<DrawingComponent> components = new Vector<>();
        String str = "dc = ";
        for(DrawingComponent component: drawingComponents) {
            str += component.getId() + " ";
        }
        Log.i("drawing", str);

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

    public void removeTexts(Text text) {
        this.texts.remove(text);
    }

    public Text findTextById(int id) {
        for(Text text: texts) {
            if(text.getTextAttribute().getId() == id) {
                return text;
            }
        }
        return null;
    }

    public int textIdCounter() {
        return ++textId;
    }

    public void addRemovedComponentIds(Vector<Integer> ids, int startIndex) {
        for(int i=startIndex; i<ids.size(); i++) {
            if(!removedComponentId.contains(ids.get(i)))
                removedComponentId.add(ids.get(i));
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
        temp.add(-1);
        for(int i=1; i<ids.size(); i++) {
            if(!removedComponentId.contains(ids.get(i)))
                temp.add(ids.get(i));
        }
        return temp;
    }

    public boolean isContainsRemovedComponentIds(Vector<Integer> ids) {
        boolean flag = true;
        for(int i=1; i<ids.size(); i++) {
            if(!removedComponentId.contains(ids.get(i))) {
                flag = false;
                break;
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

        switch(lastItem.getMode()) {
            case DRAW:
            case ERASE:
                if(drawingComponents.containsAll(lastItem.getComponents())) {
                    addRemovedComponentIds(ids, 0);
                    removeAllDrawingComponents(lastItem.getComponents());
                    eraseDrawingBoardArray(ids);
                } else {
                    for (DrawingComponent component: lastItem.getComponents()) {
                        splitPoints(component, myCanvasWidth, myCanvasHeight);
                        component.setIsErased(false);
                    }
                    removeRemovedComponentIds(ids);
                    addAllDrawingComponents(lastItem.getComponents());

                }
                Log.i("drawing", "drawingComponents.size() = " + getDrawingComponents().size());
                Log.i("drawing", "removedComponentIds = " + getRemovedComponentId());

                break;

            case SELECT:
            case GROUP:
                break;
        }
    }

    public DrawingItem popHistory() {   //undo
        int index = history.size() - 1;
        DrawingItem lastItem = history.get(index);
        history.remove(index);

        updateDrawingComponents(lastItem);

        return lastItem;
    }

    public DrawingItem popUndoArray() {  //redo
        int index = undoArray.size() - 1;
        DrawingItem lastItem = undoArray.get(index);

        updateDrawingComponents(lastItem);

        undoArray.remove(index);
        return lastItem;
    }

    public void clearUndoArray() {
        undoArray.clear();
    }

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
        try{
        drawingBoardArray = new Vector[height][width];

        for(int i=0; i<height; i++) {
            for(int j=0; j<width; j++) {
                drawingBoardArray[i][j] = new Vector<>();
                drawingBoardArray[i][j].add(-1);
            }
        }} catch(OutOfMemoryError e) {
            Log.i("mqtt", "initDrawingBoardArray");
            e.printStackTrace();
        }
    }

    public ArrayList<Point> getNewPointsById(int id) {
        return drawingBoardMap.get(id);
    }

    //drawingComponent 점 펼치기 --> drawingBoardArray
    public void splitPoints(DrawingComponent component, float canvasWidth, float canvasHeight) {
        component.calculateRatio(canvasWidth, canvasHeight);
        ArrayList<Point> newPoints = new ArrayList<>();

        try{
            switch(component.getType()) {
                case STROKE:
                    newPoints = strokeSplitPoints(component);
                    break;

                case RECT:  //직사각형 형태 모두 가능 --> rect, text, imageicon
                case OVAL:  //정교하게 수정 x^2/a^2 + y^2/b^2 <= 1
                //case TEXT:  //fixme
                    newPoints = rectSplitPoints(component);
                    break;
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
        for(DrawingComponent component: drawingComponents) {
            switch(component.getType()) {
                case STROKE:
                    break;

                case RECT:
                case OVAL:
                    Point datumPoint = component.getDatumPoint();
                    int width = component.getWidth();
                    int height = component.getHeight();
                    if((datumPoint.x <= point.x && point.x <= datumPoint.x + width) && (datumPoint.y <= point.y && point.y <= datumPoint.y + height))
                        erasedComponentIds.add(component.getId());
            }
        }
        return erasedComponentIds;
    }

    /*public ArrayList<Point> rectSplitPoints(DrawingComponent component) { //사각형안에 포함된 점 모두 --> OOM exception

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

        for(int i=datumPoint.y; i<=datumPoint.y + height; i++) {
            for(int j=datumPoint.x; j<=datumPoint.x + width; j++) {
                newPoints.add(new Point(j, i));
            }
        }

        return newPoints;

    }*/

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
        //drawAllComponents();
    }

     */



    public void eraseDrawingBoardArray(Vector<Integer> erasedComponentIds) {
        /*for(int i=1;i<erasedComponentIds.size(); i++) {
            int id = erasedComponentIds.get(i);
            removeDrawingComponents(id);
            drawAllComponents();
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

        //drawAllComponents();
        //addHistory(new DrawingItem(this.getCurrentMode(), erasedComponentIds, (drawingBitmap)));    //fixme
    }

    public void redraw() {
        //getDrawingBitmap().eraseColor(Color.TRANSPARENT);
        //drawAllComponents();

        if(history.size() == 0) {
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
    /*public void undo() {
        if(history.size() == 0)
            return;

        undoArray.add(popHistory());

        if(history.size() == 0) {
            drawingBitmap.eraseColor(Color.TRANSPARENT);
            return;
        }

        Bitmap bitmap = history.get(history.size() - 1).getBitmap();
        drawingBitmap = bitmap.copy(bitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);
    }

    public void redo() {
        if(undoArray.size() == 0)
            return;

        history.add(popUndoArray());

        Bitmap bitmap = history.get(history.size() - 1).getBitmap();
        drawingBitmap = bitmap.copy(bitmap.getConfig(), true);
        backCanvas.setBitmap(drawingBitmap);
    }*/

    public void clear() {
        /*AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("모두 지우기").setMessage("모든 그리기 내용이 삭제됩니다.\n 그래도 지우시겠습니까?");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                drawingBitmap.eraseColor(Color.TRANSPARENT);

                undoArray.clear();
                history.clear();
                drawingComponents.clear();
                initDrawingBoardArray((int)myCanvasWidth, (int)myCanvasHeight);
                drawingBoardMap.clear();

                setDrawingView(((MainActivity) getContext()).getDrawingView());
                drawingView.invalidate();
                Log.i("drawing", "history.size()=" + getHistory().size());

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
        alertDialog.show();*/
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


    public void setContext(Context context) {
        this.context = context;
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

    /*public void setComponentBitmap(Bitmap componentBitmap) {
        this.componentBitmap = componentBitmap;
    }*/
}
