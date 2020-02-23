package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;
import android.util.Log;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;

import java.util.TreeSet;
import java.util.Vector;


public class Eraser {
    private DrawingEditor de = DrawingEditor.getInstance();
    private MQTTClient client = MQTTClient.getInstance();
    private JSONParser parser = JSONParser.getInstance();
    private int squareScope = 20;//(int) (((de.getMyCanvasWidth() * de.getMyCanvasHeight()) * 0.0014556040756914) / 100);

    public void findComponentsToErase(Point eraserPoint) {
        Vector<Integer> erasedComponentIds = new Vector<>();

        int x = eraserPoint.x;
        int y = eraserPoint.y;

        Vector<Integer>[][] dbArray = de.getDrawingBoardArray();

        if(y-squareScope<0 || x-squareScope<0 || y+squareScope>de.getMyCanvasHeight() || x+squareScope>de.getMyCanvasWidth()) {
            Log.i("drawing", "eraser exit");
            return;
        }

        /*if(de.findEnclosingDrawingComponents(eraserPoint).size() != 1 && !de.isContainsRemovedComponentIds(dbArray[y][x])) {
            erasedComponentIds.addAll(de.findEnclosingDrawingComponents(eraserPoint));
            //erase(erasedComponentIds);
        }*/

        for(int i=y-squareScope; i<y+squareScope; i++) {
            for(int j=x-squareScope; j<x+squareScope; j++) {
                if(dbArray[i][j].size() != 1 && !de.isContainsRemovedComponentIds(dbArray[i][j])) { //-1만 가지고 있으면 size() == 1
                    //erasedComponentIds = (dbArray[i][j]);
                    erasedComponentIds.addAll(de.getNotRemovedComponentIds(dbArray[i][j]));

                    if(de.findEnclosingDrawingComponents(eraserPoint).size() != 1) {
                        erasedComponentIds.addAll(de.findEnclosingDrawingComponents(eraserPoint));
                    }

                    erasedComponentIds = new Vector<>(new TreeSet<>(erasedComponentIds));
                    erase(erasedComponentIds);
                }
            }
        }

    }

    public void erase(Vector<Integer> erasedComponentIds) {
        de.addRemovedComponentIds(erasedComponentIds, 1);

        Log.i("drawing", "erasedIds = " + erasedComponentIds.toString());

        //publish
        MqttMessageFormat messageFormat = new MqttMessageFormat(de.getMyUsername(), de.getCurrentMode(), erasedComponentIds);
        client.publish(client.getTopic_data(), parser.jsonWrite(messageFormat));

        //de.eraseDrawingComponents(erasedComponentIds);
        new EraserTask(erasedComponentIds).execute();
    }

}
