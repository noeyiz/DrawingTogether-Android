package com.hansung.drawingtogether.view.drawing;

import java.util.Vector;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MqttMessageFormat {
    private Mode mode;
    private ComponentType type;
    private DrawingComponent component;
    private int action;

    private String username;
    private Vector<Integer> componentIds; //findComponentsToErase, select

    private TextAttribute textAttr; // text
    private TextMode textMode;

    // private ArrayList<Point> sendingPoints; // 점을 두개 이상 보낼 경우 사용할 자료구조

    public MqttMessageFormat(String username, Mode mode, ComponentType type, DrawingComponent component, int action) {
        this.username = username;
        this.mode = mode;
        this.type = type;
        this.component = component;
        this.action  = action;
    }

    public MqttMessageFormat(String username, Mode mode, Vector<Integer> componentIds) {
        this.username = username;
        this.mode = mode;
        this.componentIds = componentIds;
    }

    public MqttMessageFormat(String username, Mode mode, ComponentType type, TextAttribute textAttr, TextMode textMode) {
        this.username = username;
        this.mode = mode;
        this.type = type;
        this.textAttr = textAttr;
        this.textMode = textMode;
    }
}
