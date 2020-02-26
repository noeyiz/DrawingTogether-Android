package com.hansung.drawingtogether.view.drawing;

import com.hansung.drawingtogether.view.main.JoinMessage;

import java.util.ArrayList;

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

    private JoinMessage joinMessage;

    // fixme nayeon 중간자 처리 시 필요한 변수 추가
    private ArrayList<DrawingComponent> drawingComponents;
    private ArrayList<Text> texts;
    private byte[] bitmapByteArray;

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

    public MqttMessageFormat(String username, Mode mode) {
        this.username = username;
        this.mode = mode;
    }
/*

    public MqttMessageFormat(String username, Mode mode,  byte[] bitmapByteArray) { // 이미지 전송 시 사용할 생성자
        this.username = username;
        this.mode = mode;
        this.bitmapByteArray = bitmapByteArray;
    }

*/
    // fixme nayeon 중간자 처리 시 필요한 생성자 3개 추가
    public MqttMessageFormat(JoinMessage joinMessage, ArrayList<DrawingComponent> drawingComponents, ArrayList<Text> texts) {
        this.joinMessage = joinMessage;
        this.drawingComponents = drawingComponents;
        this.texts = texts;
    }

    public MqttMessageFormat(JoinMessage joinMessage, ArrayList<DrawingComponent> drawingComponents, ArrayList<Text> texts, byte[] bitmapByteArray) {
        this.joinMessage = joinMessage;
        this.drawingComponents = drawingComponents;
        this.texts = texts;
        this.bitmapByteArray = bitmapByteArray;
    }

    public MqttMessageFormat(JoinMessage joinMessage) {
        this.joinMessage = joinMessage;
    }
}
