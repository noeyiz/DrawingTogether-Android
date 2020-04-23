package com.hansung.drawingtogether.view.drawing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Getter;

@Getter
public enum JSONParser {
    INSTANCE;

    private Gson gson;
    //private DrawingComponentAdapter drawingComponentAdapter;

    public static JSONParser getInstance() { return INSTANCE; }

    private JSONParser() { }

    public void initJsonParser(DrawingFragment drawingFragment) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        //drawingComponentAdapter = new DrawingComponentAdapter();
        gsonBuilder.registerTypeAdapter(DrawingComponent.class, new DrawingComponentAdapter());
        gsonBuilder.registerTypeAdapter(Text.class, new TextAdapter(drawingFragment));
        gson = gsonBuilder.create();
    }

    public /*synchronized*/ String jsonWrite(Object object) { return gson.toJson(object); }

    public Object jsonReader(String jsonString) { return gson.fromJson(jsonString, MqttMessageFormat.class); }
}

