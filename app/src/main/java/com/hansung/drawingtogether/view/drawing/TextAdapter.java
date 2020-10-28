package com.hansung.drawingtogether.view.drawing;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hansung.drawingtogether.data.remote.model.MyLog;

import java.lang.reflect.Type;

// Parse Only TextAttribute
// Text 객체 파싱은 사실상 TextAttribute 객체 파싱
public class TextAdapter implements JsonSerializer<Text>, JsonDeserializer<Text> {

    private final String TEXT_ATTRIBUTE = "textAttr";
    private DrawingFragment drawingFragment;

    public TextAdapter(DrawingFragment drawingFragment) { this.drawingFragment = drawingFragment; }

    @Override
    public JsonElement serialize(Text src, Type typeOfSrc, JsonSerializationContext context) {
        TextAttribute textAttribute = src.getTextAttribute();

        JsonObject result = new JsonObject();
        try {
            result.add(TEXT_ATTRIBUTE, context.serialize(textAttribute, textAttribute.getClass()));
        }catch (JsonIOException jioe) {
            MyLog.e("json", "SERIALIZE ERROR " + jioe.getCause().toString());
        }
        return result;
    }

    @Override
    public Text deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        JsonElement element = jsonObject.get(TEXT_ATTRIBUTE);
        TextAttribute textAttribute = context.deserialize(element, TextAttribute.class);
        return new Text(drawingFragment, textAttribute);
    }
}