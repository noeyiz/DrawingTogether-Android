package com.hansung.drawingtogether.view.drawing;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class TextAttribute {

    // 사용자가 선택한 텍스트 속성 저장

    private String id; // "이름 + textIdCounter"
    private String username;

    private String preText;
    private String postText;
    private String text = "";
    private int textSize;
    private int textColor;
    private int textBackgroundColor;
    private int textGravity;
    private int style;

    private int generatedLayoutWidth;
    private int generatedLayoutHeight;

    private int preX;
    private int preY;
    private int postX;
    private int postY;
    private int x;
    private int y;

    private boolean isModified = false;

    private boolean isTextInited = false;
    private boolean isTextMoved = false;

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

        this.preText = textAttr.preText;
        this.postText = textAttr.postText;
        this.text = textAttr.text;
        this.textSize = textAttr.textSize;
        this.textColor = textAttr.textColor;
        this.textBackgroundColor = textAttr.textBackgroundColor;
        this.textGravity = textAttr.textGravity;
        this.style = textAttr.style;
        this.generatedLayoutWidth = textAttr.generatedLayoutWidth;
        this.generatedLayoutHeight = textAttr.generatedLayoutHeight;

        this.preX = textAttr.preX;
        this.preY = textAttr.preY;
        this.postX = textAttr.postX;
        this.postY = textAttr.postY;
        this.x = textAttr.x;
        this.y = textAttr.y;
    }

    public TextAttribute(String id, String username, int textSize, int textColor, int textBackgroundColor,
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


    public void setCoordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setPreCoordinates(int preX, int preY) {
        this.preX = preX;
        this.preY = preY;
    }
}
