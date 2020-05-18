package com.hansung.drawingtogether.view.main;

import android.util.Log;
import android.view.MotionEvent;

import lombok.Getter;

// fixme hyeyeon
@Getter
public class WarpingMessage {
    private int action;
    private float x, y;
    private float[] multiX = new float[2];
    private float[] multiY = new float[2];
    private int[] id = new int[2];
    private int[] toolType = new int[2];
    private float[] orientation = new float[2];
    private float[] pressure = new float[2];
    private float[] size = new float[2];
    private float[] toolMajor = new float[2];
    private float[] toolMinor = new float[2];
    private float[] touchMajor = new float[2];
    private float[] touchMinor = new float[2];
    private int metaState;
    private int buttonState;
    private float xPrecision;
    private float yPrecision;
    private int deviceId;
    private int edgeFlags;
    private int source;
    private int flags;
    private int pointerCount;

    public WarpingMessage(MotionEvent event) {
        action = event.getAction();
        pointerCount = event.getPointerCount();
        x = event.getX();
        y = event.getY();
        for (int i = 0; i < pointerCount; i++) {
            multiX[i] = event.getX(i);
            multiY[i] = event.getY(i);
            id[i] = event.getPointerId(i);
            toolType[i] = event.getToolType(i);
            orientation[i] = event.getOrientation(i);
            pressure[i] = event.getPressure(i);
            size[i] = event.getSize(i);
            toolMajor[i] = event.getToolMajor(i);
            toolMinor[i] = event.getToolMinor(i);
            touchMajor[i] = event.getTouchMajor(i);
            touchMinor[i] = event.getTouchMinor(i);
        }
        metaState = event.getMetaState();
        buttonState = event.getButtonState();
        xPrecision = event.getXPrecision();
        yPrecision = event.getYPrecision();
        deviceId = event.getDeviceId();
        edgeFlags = event.getEdgeFlags();
        source = event.getSource();
        flags = event.getFlags();
    }

    private MotionEvent.PointerProperties[] getPointerProperties() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[2];

        for (int i = 0; i < pointerCount; i ++) {
            pointerProperties[i] = new MotionEvent.PointerProperties();
            pointerProperties[i].id = id[i];
            pointerProperties[i].toolType = toolType[i];
        }
        return pointerProperties;
    }

    private MotionEvent.PointerCoords[] getPointerCoords() {
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[2];
        for (int i = 0; i < pointerCount; i++) {
            pointerCoords[i] = new MotionEvent.PointerCoords();
            pointerCoords[i].orientation = orientation[i];
            pointerCoords[i].pressure = pressure[i];
            pointerCoords[i].size = size[i];
            pointerCoords[i].toolMajor = toolMajor[i];
            pointerCoords[i].toolMinor = toolMinor[i];
            pointerCoords[i].touchMajor = touchMajor[i];
            pointerCoords[i].touchMinor = touchMinor[i];
            pointerCoords[i].x = multiX[i];
            pointerCoords[i].y = multiY[i];
        }
        return pointerCoords;
    }

    public MotionEvent getEvent() {
        MotionEvent event = null;
        if (pointerCount > 1) {
            MotionEvent.PointerProperties[] pointerProperties = getPointerProperties();
            MotionEvent.PointerCoords[] pointerCoords = getPointerCoords();

            event = MotionEvent.obtain(
                    System.currentTimeMillis(),
                    System.currentTimeMillis() + 100,
                    action,
                    pointerCount,
                    pointerProperties,
                    pointerCoords,
                    metaState,
                    buttonState,
                    xPrecision,
                    yPrecision,
                    deviceId,
                    edgeFlags,
                    source,
                    flags
            );
        }
        else {
            event = MotionEvent.obtain(
                    0,
                    0,
                    action,
                    x,
                    y,
                    0
            );
        }
        Log.e("event", event + "");
        return event;
    }
}
