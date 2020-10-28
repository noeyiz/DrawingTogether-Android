package com.hansung.drawingtogether.view.drawing;

import android.view.Gravity;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class TextAttribute {

    // 사용자가 선택한 텍스트 속성 저장

    private String id; // "이름 + textIdCounter"
    private String username;

    private String text = "";
    private int textSize;
    private String textColor;

    private int generatedLayoutWidth;
    private int generatedLayoutHeight;

    private int x;
    private int y;

    private boolean isTextInited = false;
    private boolean isTextMoved = false;

    private boolean isTextChangedColor = false;

    private boolean isDragging = false;
/*
    public TextAttribute(int id, String username, String text, int textSize, int textColor, int textBackgroundColor,
                         int textGravity, int style, int generatedLayoutWidth, int generatedLayoutHeight) {
        this.id = id;
        this.username = username;
        this.text = text;
        this.textSize = textSize;
        this.textColor = textColor;
        this.textBackgroundColor = textBackgroundColor;
        this.textGravity = textGravity;
        this.style = style;
        this.generatedLayoutWidth = generatedLayoutWidth;
        this.generatedLayoutHeight = generatedLayoutHeight;
    }
*/

    public TextAttribute(TextAttribute textAttr) {
        this.id = textAttr.id;
        this.username = textAttr.username;

        this.text = textAttr.text;
        this.textSize = textAttr.textSize;
        this.textColor = textAttr.textColor;

        this.generatedLayoutWidth = textAttr.generatedLayoutWidth;
        this.generatedLayoutHeight = textAttr.generatedLayoutHeight;

        this.x = textAttr.x;
        this.y = textAttr.y;
    }

    public TextAttribute(String id, String username, int textSize, String textColor, int generatedLayoutWidth, int generatedLayoutHeight) {

        this.id = id;
        this.username = username;

        this.textSize = textSize;
        this.textColor = textColor;

        this.generatedLayoutWidth = generatedLayoutWidth;
        this.generatedLayoutHeight = generatedLayoutHeight;
    }

    public void setCoordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

}