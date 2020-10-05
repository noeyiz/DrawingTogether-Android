package com.hansung.drawingtogether.view.main;

import lombok.Getter;

@Getter
public class AutoDrawMessage {
    private String name;
    private String url;
    private float x, y;

    public AutoDrawMessage(String name, String url, float x, float y) {
        this.name = name;
        this.url = url;
        this.x = x;
        this.y = y;
    }
}