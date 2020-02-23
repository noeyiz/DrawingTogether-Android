package com.hansung.drawingtogether.view.drawing;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class TextAttribute {

    // 사용자가 선택한 텍스트 속성 저장

    private int id;
    private String username;

    private String text;
    private int textSize;
    private int textColor;
    private int textBackgroundColor;
    private int textGravity;
    private int style;

    private int generatedLayoutWidth;
    private int generatedLayoutHeight;

    private int x;
    private int y;

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

    public void setCoordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
