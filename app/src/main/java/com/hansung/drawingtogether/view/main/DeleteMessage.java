package com.hansung.drawingtogether.view.main;

import lombok.Getter;

// fixme hyeyeon
@Getter
public class DeleteMessage {
    String name;
    String message;

    public DeleteMessage(String name) {
        this.name = name;
    }

    public DeleteMessage(String name, String message) {
        this.name = name;
        this.message = message;
    }
}
