package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;

import com.hansung.drawingtogether.data.remote.model.User;
import com.hansung.drawingtogether.view.main.AliveMessage;
import com.hansung.drawingtogether.view.main.DeleteMessage;
import com.hansung.drawingtogether.view.main.ExitMessage;
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
    private Integer id;
    private String usersComponentId;
    private Point point;

    private String username;
    private Vector<Integer> componentIds; //findComponentsToErase, findSelectedComponent

    private TextAttribute textAttr; // text
    private TextMode textMode;

    private int myTextArrayIndex;

    private JoinMessage joinMessage;


    // fixme hyeyeon
    private ExitMessage exitMessage;
    private DeleteMessage deleteMessage;
    private AliveMessage aliveMessage;
    //

    // fixme nayeon 중간자 처리 시 필요한 변수 추가
    private ArrayList<DrawingComponent> drawingComponents;
    private ArrayList<Text> texts;
    private ArrayList<DrawingItem> history;
    private ArrayList<DrawingItem> undoArray;
    private Vector<Integer> removedComponentId;
    private Integer maxComponentId;
    private Integer maxTextId;
    private byte[] bitmapByteArray;

    public MqttMessageFormat(String username, String usersComponentId, Mode mode, ComponentType type, DrawingComponent component, int action) {
        this.username = username;
        this.usersComponentId = usersComponentId;
        this.mode = mode;
        this.type = type;
        this.component = component;
        this.action  = action;
    }

    public MqttMessageFormat(String username, Integer id, String usersComponentId, Mode mode, ComponentType type, Point point, int action) {
        this.username = username;
        this.id = id;
        this.usersComponentId = usersComponentId;
        this.mode = mode;
        this.type = type;
        this.point = point;
        this.action  = action;
    }

    public MqttMessageFormat(String username, Mode mode, Vector<Integer> componentIds) {
        this.username = username;
        this.mode = mode;
        this.componentIds = componentIds;
    }

    // fixme nayeon - 텍스트 동시성 처리
    public MqttMessageFormat(String username, Mode mode, ComponentType type, TextAttribute textAttr, TextMode textMode, int myTextArrayIndex) {
        this.username = username;
        this.mode = mode;
        this.type = type;
        this.textAttr = textAttr;
        this.textMode = textMode;
        this.myTextArrayIndex = myTextArrayIndex;
    }

    public MqttMessageFormat(String username, Mode mode) {
        this.username = username;
        this.mode = mode;
    }

    public MqttMessageFormat(String username, Mode mode,  byte[] bitmapByteArray) { // 이미지 전송 시 사용할 생성자
        this.username = username;
        this.mode = mode;
        this.bitmapByteArray = bitmapByteArray;
    }

    //fixme nayeon 중간자 처리 시 필요한 생성자 3개 추가
    //fixme minj - add history for undo, redo
    public MqttMessageFormat(JoinMessage joinMessage, ArrayList<DrawingComponent> drawingComponents, ArrayList<Text> texts, ArrayList<DrawingItem> history, ArrayList<DrawingItem> undoArray, Vector<Integer> removedComponentId, Integer maxComponentId, Integer maxTextId) {
        this.joinMessage = joinMessage;
        this.drawingComponents = drawingComponents;
        this.texts = texts;
        this.history = history;
        this.undoArray = undoArray;
        this.removedComponentId = removedComponentId;
        this.maxComponentId = maxComponentId;
        this.maxTextId = maxTextId;
    }

    public MqttMessageFormat(JoinMessage joinMessage, ArrayList<DrawingComponent> drawingComponents, ArrayList<Text> texts, ArrayList<DrawingItem> history, ArrayList<DrawingItem> undoArray,  Vector<Integer> removedComponentId, Integer maxComponentId, Integer maxTextId, byte[] bitmapByteArray) {
        this.joinMessage = joinMessage;
        this.drawingComponents = drawingComponents;
        this.texts = texts;
        this.history = history;
        this.undoArray = undoArray;
        this.removedComponentId = removedComponentId;
        this.maxComponentId = maxComponentId;
        this.maxTextId = maxTextId;
        this.bitmapByteArray = bitmapByteArray;
    }

    public MqttMessageFormat(JoinMessage joinMessage) {
        this.joinMessage = joinMessage;
    }

    // fixme hyeyeon
    public MqttMessageFormat(ExitMessage exitMessage) {
        this.exitMessage = exitMessage;
    }

    public MqttMessageFormat(DeleteMessage deleteMessage) {
        this.deleteMessage = deleteMessage;
    }

    public MqttMessageFormat(AliveMessage aliveMessage) {
        this.aliveMessage = aliveMessage;
    }
    //
}
