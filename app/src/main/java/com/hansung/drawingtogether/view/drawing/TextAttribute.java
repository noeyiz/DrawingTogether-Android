package com.hansung.drawingtogether.view.drawing;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class TextAttribute {

    // 사용자가 선택한 텍스트 속성 저장

    //private Integer id; // 텍스트 동시 제어를 위해 ID 값을 NULL 처리 할 필요
    private String id; // "이름 + textIdCounter"
    private String username;

    private String preText;
    private String postText;
    private String text;
    private int textSize;
    private int textColor;
    private int textBackgroundColor;
    private int textGravity;
    private int style;

    private int generatedLayoutWidth;
    private int generatedLayoutHeight;

    private int preX;
    private int preY;
    private int x;
    private int y;

    private boolean isTextInited = false; // fixme nayeon (Text class -> TextAttribute Class)

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


    public TextAttribute(String id, String username, String text, int textSize, int textColor, int textBackgroundColor,
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
