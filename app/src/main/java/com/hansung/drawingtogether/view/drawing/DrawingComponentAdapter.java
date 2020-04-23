package com.hansung.drawingtogether.view.drawing;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

// 추상 클래스인 DrawindComponent 에 대한 객체를 처리하기 위한 Adapter
public class DrawingComponentAdapter
        implements JsonSerializer<DrawingComponent>, JsonDeserializer<DrawingComponent> {

    private final String CLASSNAME = "CLASSNAME";
    private final String OBJECT = "OBJECT";


    @Override
    public /*synchronized*/ JsonElement serialize(DrawingComponent src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.add(CLASSNAME, new JsonPrimitive(src.getClass().getSimpleName()));
        JsonElement je = context.serialize(src, src.getClass());    //****
        result.add(OBJECT, je);      //fixme min

        return result;
    }

    @Override
    public DrawingComponent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get(CLASSNAME).getAsString();
        JsonElement element = jsonObject.get(OBJECT);

        try { return context.deserialize(element, Class.forName(getPackageName() + "." + type)); }
        catch (ClassNotFoundException e) { throw new JsonParseException(e.getMessage()); }
    }

    private String getPackageName() { return DrawingComponent.class.getPackage().getName(); }
}
