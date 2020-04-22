package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.Log; // fixme nayeon

import java.util.TreeSet;
import java.util.Vector;

import lombok.Getter;

@Getter
public class Eraser {
    private DrawingEditor de = DrawingEditor.getInstance();
    private MQTTClient client = MQTTClient.getInstance();
    private JSONParser parser = JSONParser.getInstance();
    private int squareScope = 20;//(int) (((de.getMyCanvasWidth() * de.getMyCanvasHeight()) * 0.0014556040756914) / 100);
    private Vector<Integer> erasedComponentIds;

    public void findComponentsToErase(Point eraserPoint) {
        erasedComponentIds = new Vector<>();
        erasedComponentIds.add(-1);

        int x = eraserPoint.x;
        int y = eraserPoint.y;

        Vector<Integer>[][] dbArray = de.getDrawingBoardArray();

        if(y-squareScope<0 || x-squareScope<0 || y+squareScope>de.getMyCanvasHeight() || x+squareScope>de.getMyCanvasWidth()) {
            Log.i("drawing", "eraser exit");
            return;
        }

        for(int i=y-squareScope; i<y+squareScope; i++) {
            for(int j=x-squareScope; j<x+squareScope; j++) {
                if(de.findEnclosingDrawingComponents(eraserPoint).size() != 1 && !de.isContainsRemovedComponentIds(de.findEnclosingDrawingComponents(eraserPoint))) {
                    erasedComponentIds.addAll(de.findEnclosingDrawingComponents(eraserPoint));
                    de.addRemovedComponentIds(de.findEnclosingDrawingComponents(eraserPoint));
                    Log.i("drawing", "erased shape ids = " + erasedComponentIds.toString());
                    //erase(erasedComponentIds);
                }

                if(dbArray[i][j].size() != 1 && !de.isContainsRemovedComponentIds(dbArray[i][j])) { //-1만 가지고 있으면 size() == 1
                    //erasedComponentIds = (dbArray[i][j]);
                    erasedComponentIds.addAll(de.getNotRemovedComponentIds(dbArray[i][j]));
                    de.addRemovedComponentIds(de.getNotRemovedComponentIds(dbArray[i][j]));
                    Log.i("drawing", "erased stroke ids = " + erasedComponentIds.toString());

                    /*if(de.findEnclosingDrawingComponents(eraserPoint).size() != 1) {
                        erasedComponentIds.addAll(de.findEnclosingDrawingComponents(eraserPoint));
                    }*/
                }

            }
        }

        if(erasedComponentIds.size() != 1) {
            erasedComponentIds = new Vector<>(new TreeSet<>(erasedComponentIds));
            erase(erasedComponentIds);
        }
    }

    public void erase(Vector<Integer> erasedComponentIds) {
        Log.i("drawing", "erasedIds = " + erasedComponentIds.toString());

        //publish
        MqttMessageFormat messageFormat = new MqttMessageFormat(de.getMyUsername(), de.getCurrentMode(), erasedComponentIds);
        client.publish(client.getTopic_data(), parser.jsonWrite(messageFormat));

        //de.eraseDrawingComponents(erasedComponentIds);
        new EraserTask(erasedComponentIds).execute();
        erasedComponentIds.clear();
        de.clearUndoArray();
    }
}
