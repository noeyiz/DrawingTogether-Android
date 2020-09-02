package com.hansung.drawingtogether.view.main;

import lombok.Getter;

@Getter
public class AliveMessage {
    private String name;

    public AliveMessage(String name) {
        this.name = name;
    }
}
