package com.hansung.drawingtogether.view.drawing;

import android.graphics.Point;

import com.hansung.drawingtogether.monitoring.ComponentCount;
import com.hansung.drawingtogether.view.main.AliveMessage;
import com.hansung.drawingtogether.view.main.AudioMessage;
import com.hansung.drawingtogether.view.main.AutoDrawMessage;
import com.hansung.drawingtogether.view.main.CloseMessage;
import com.hansung.drawingtogether.view.main.ExitMessage;
import com.hansung.drawingtogether.view.main.JoinAckMessage;
import com.hansung.drawingtogether.view.main.JoinMessage;
import com.hansung.drawingtogether.view.main.WarpingMessage;

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
    private Integer action;
    private Integer id;
    private String usersComponentId;
    private Point point;
    private ArrayList<Point> movePoints;
    private Boolean isSelected;

    private String username;
    private Vector<Integer> componentIds; //findComponentsToErase, findSelectedComponent

    private TextAttribute textAttr; // text
    private TextMode textMode;

    private Integer myTextArrayIndex;

    private JoinMessage joinMessage;
    private JoinAckMessage joinAckMessage;
    private ExitMessage exitMessage;
    private CloseMessage closeMessage;
    private AliveMessage aliveMessage;
    private AudioMessage audioMessage;
    private WarpingMessage warpingMessage;
    private AutoDrawMessage autoDrawMessage;

    private ArrayList<DrawingComponent> drawingComponents;
    private ArrayList<Text> texts;
    private ArrayList<DrawingItem> history;
    private ArrayList<DrawingItem> undoArray;
    private Vector<Integer> removedComponentId;
    private Integer maxComponentId;
    private Integer maxTextId;
    private byte[] bitmapByteArray;

    private ComponentCount componentCount;

    /* Draw - Action Down */
    public MqttMessageFormat(String username, String usersComponentId, Mode mode, ComponentType type, DrawingComponent component, int action) {
        this.username = username;
        this.usersComponentId = usersComponentId;
        this.mode = mode;
        this.type = type;
        this.component = component;
        this.action  = action;
    }

    /* Draw - Action Up */
    public MqttMessageFormat(String username, /*Integer id, */String usersComponentId, Mode mode, ComponentType type, Point point, int action) {
        this.username = username;
        this.id = id;
        this.usersComponentId = usersComponentId;
        this.mode = mode;
        this.type = type;
        this.point = point;
        this.action  = action;
    }

    /* Erase */
    public MqttMessageFormat(String username, Mode mode, Vector<Integer> componentIds) {
        this.username = username;
        this.mode = mode;
        this.componentIds = componentIds;
    }

    /* Draw - Move Chunk */
    public MqttMessageFormat(String username, /*Integer id, */String usersComponentId, Mode mode, ComponentType type, ArrayList<Point> movePoints, int action) {
        this.username = username;
        //this.id = id;
        this.usersComponentId = usersComponentId;
        this.mode = mode;
        this.type = type;
        this.movePoints = movePoints;
        this.action  = action;
    }

    /* Select, Deselect */
    public MqttMessageFormat(String username, String usersComponentId, Mode mode, boolean isSelected) {
        this.username = username;
        this.usersComponentId = usersComponentId;
        this.mode = mode;
        this.isSelected = isSelected;
    }

    /* Select - Down, Move, Up */
    ArrayList<Point> moveSelectPoints;
    public MqttMessageFormat(String username, String usersComponentId, Mode mode, int action, ArrayList<Point> moveSelectPoints) {
        this.username = username;
        this.usersComponentId = usersComponentId;
        this.mode = mode;
        this.action = action;
        this.moveSelectPoints = moveSelectPoints;
    }

    public MqttMessageFormat(String username, Mode mode, ComponentType type, TextAttribute textAttr, TextMode textMode, int myTextArrayIndex) {
        this.username = username;
        this.mode = mode;
        this.type = type;
        this.textAttr = textAttr;
        this.textMode = textMode;
        this.myTextArrayIndex = myTextArrayIndex;
    }

    /* Mode Change */
    public MqttMessageFormat(String username, Mode mode) {
        this.username = username;
        this.mode = mode;
    }

    /* 이미지 전송 시 필요한 생성자 */
    public MqttMessageFormat(String username, Mode mode,  byte[] bitmapByteArray) {
        this.username = username;
        this.mode = mode;
        this.bitmapByteArray = bitmapByteArray;
    }

    /* 중간자 처리시 필요한 생성자 */
    public MqttMessageFormat(JoinAckMessage joinAckMessage, ArrayList<DrawingComponent> drawingComponents, ArrayList<Text> texts, ArrayList<DrawingItem> history, ArrayList<DrawingItem> undoArray, Vector<Integer> removedComponentId, Integer maxComponentId, Integer maxTextId) {
        this.joinAckMessage = joinAckMessage;
        this.drawingComponents = drawingComponents;
        this.texts = texts;
        this.history = history;
        this.undoArray = undoArray;
        this.removedComponentId = removedComponentId;
        this.maxComponentId = maxComponentId;
        this.maxTextId = maxTextId;
    }

    /* 중간자 처리시 필요한 생성자 (BitmapByteArray 포함) */
    public MqttMessageFormat(JoinAckMessage joinAckMessage, ArrayList<DrawingComponent> drawingComponents, ArrayList<Text> texts, ArrayList<DrawingItem> history, ArrayList<DrawingItem> undoArray, Vector<Integer> removedComponentId, Integer maxComponentId, Integer maxTextId, byte[] bitmapByteArray) {
        this.joinAckMessage = joinAckMessage;
        this.drawingComponents = drawingComponents;
        this.texts = texts;
        this.history = history;
        this.undoArray = undoArray;
        this.removedComponentId = removedComponentId;
        this.maxComponentId = maxComponentId;
        this.maxTextId = maxTextId;
        this.bitmapByteArray = bitmapByteArray;
    }

    public MqttMessageFormat(String username, Mode mode, ComponentType type, int action, WarpingMessage warpingMessage) {
        this.username = username;
        this.mode = mode;
        this.type = type;
        this.action  = action;
        this.warpingMessage = warpingMessage;
    }

    public MqttMessageFormat(String username, Mode mode, ComponentType type, AutoDrawMessage autoDrawMessage) {
        this.username = username;
        this.mode = mode;
        this.type = type;
        this.autoDrawMessage = autoDrawMessage;
    }
    public MqttMessageFormat(ComponentCount componentCount) {
        this.componentCount = componentCount;
    }

    public MqttMessageFormat(JoinMessage joinMessage) {
        this.joinMessage = joinMessage;
    }

    public MqttMessageFormat(JoinAckMessage joinAckMessage) {
        this.joinAckMessage = joinAckMessage;
    }

    public MqttMessageFormat(ExitMessage exitMessage) {
        this.exitMessage = exitMessage;
    }

    public MqttMessageFormat(CloseMessage closeMessage) {
        this.closeMessage = closeMessage;
    }

    public MqttMessageFormat(AliveMessage aliveMessage) {
        this.aliveMessage = aliveMessage;
    }

    public MqttMessageFormat(AudioMessage audioMessage) {
        this.audioMessage = audioMessage;
    }

}
