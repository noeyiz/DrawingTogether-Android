package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;

import lombok.Getter;

@Getter
public class Selector {
    private DrawingEditor de = DrawingEditor.getInstance();
    private MQTTClient client = MQTTClient.getInstance();
    private JSONParser parser = JSONParser.getInstance();
    private int squareScope = 20;//(int) (((de.getMyCanvasWidth() * de.getMyCanvasHeight()) * 0.0014556040756914) / 100);
    private int selectedComponentId = -1;

    public void findSelectedComponent(Point selectorPoint) {
        selectedComponentId = -1;

        int x = selectorPoint.x;
        int y = selectorPoint.y;

        //Vector<Integer>[][] dbArray = de.getDrawingBoardArray();

        if(y-squareScope<0 || x-squareScope<0 || y+squareScope>de.getMyCanvasHeight() || x+squareScope>de.getMyCanvasWidth()) {
            MyLog.i("drawing", "selector exit");
            return;
        }


        if(de.findEnclosingDrawingComponents(selectorPoint).size() != 0 && !de.isContainsRemovedComponentIds(de.findEnclosingDrawingComponents(selectorPoint))) {

            selectedComponentId = de.findEnclosingDrawingComponents(selectorPoint).lastElement();
            MyLog.i("drawing", "selected shape ids = " + selectedComponentId);
        }

        /*for(int i=y-squareScope; i<y+squareScope; i++) {    //맨 위에 그려진 선 or 도형 id --> 선과 도형이 동시에 잡힌경우 도형 id
            for(int j=x-squareScope; j<x+squareScope; j++) {
                if(dbArray[i][j].size() != 1 && !de.isContainsRemovedComponentIds(dbArray[i][j])) { //-1만 가지고 있으면 size() == 1
                    selectedComponentId = de.getNotRemovedComponentIds(dbArray[i][j]).lastElement();
                    Log.i("drawing", "selected stroke ids = " + selectedComponentId);
                }

                if(de.findEnclosingDrawingComponents(selectorPoint).size() != 1 && !de.isContainsRemovedComponentIds(de.findEnclosingDrawingComponents(selectorPoint))) {
                    selectedComponentId = de.findEnclosingDrawingComponents(selectorPoint).lastElement();
                    Log.i("drawing", "selected shape ids = " + selectedComponentId);
                    return;
                }
            }
        }*/

    }
}
