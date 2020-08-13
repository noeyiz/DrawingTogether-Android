package com.hansung.drawingtogether.view.main;

import lombok.Getter;

@Getter
public class JoinAckMessage {
    private String name;
    private String target;

    private float drawnCanvasWidth;
    private float drawnCanvasHeight;

    public JoinAckMessage(String name, String target, float drawnCanvasWidth, float drawnCanvasHeight) {
        this.name = name;
        this.target = target;

        this.drawnCanvasWidth = drawnCanvasWidth;
        this.drawnCanvasHeight = drawnCanvasHeight;
    }

}
