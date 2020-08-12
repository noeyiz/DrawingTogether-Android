package com.hansung.drawingtogether.data.remote.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// fixme hyeyeon
@Getter
@Setter
public class User {
    private String name;
    private int count;
    private int action;
    private boolean isInitialized;

    private float drawnCanvasWidth;
    private float drawnCanvasHeight;

    public User(String name, int count, int action, boolean isInitialized) {
        this.name = name;
        this.count = count;
        this.action = action;
        this.isInitialized = isInitialized;
    }

    public User(String name, int count, int action, boolean isInitialized, float drawnCanvasWidth, float drawnCanvasHeight) {
        this.name = name;
        this.count = count;
        this.action = action;
        this.isInitialized = isInitialized;

        this.drawnCanvasWidth = drawnCanvasWidth;
        this.drawnCanvasHeight = drawnCanvasHeight;
    }

    public void setDrawnCanvasSize(float drawnCanvasWidth, float drawnCanvasHeight) {
        this.drawnCanvasWidth = drawnCanvasWidth;
        this.drawnCanvasHeight = drawnCanvasHeight;
    }

}
