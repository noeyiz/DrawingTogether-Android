package com.hansung.drawingtogether.view.drawing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public enum JSONParser {
    INSTANCE;

    private Gson gson;

    public static JSONParser getInstance() { return INSTANCE; }

    private JSONParser() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(DrawingComponent.class, new DrawingComponentAdapter());
        gson = gsonBuilder.create();
    }

    public String jsonWrite(Object object) { return gson.toJson(object); }

    public Object jsonReader(String jsonString) { return gson.fromJson(jsonString, MqttMessageFormat.class); }
}

