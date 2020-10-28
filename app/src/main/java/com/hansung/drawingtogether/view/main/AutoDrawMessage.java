package com.hansung.drawingtogether.view.main;

import lombok.Getter;

@Getter
public class AutoDrawMessage {
    private String name;
    private String url;
    private float x, y;
    private float width, height;

    public AutoDrawMessage(String name, String url, float x, float y, float width, float height) {
        this.name = name;
        this.url = url;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}