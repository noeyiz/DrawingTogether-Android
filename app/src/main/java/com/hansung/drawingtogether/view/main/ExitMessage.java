package com.hansung.drawingtogether.view.main;

import lombok.Getter;

// fixme hyeyeon
@Getter
public class ExitMessage {
    String name;
    String message;

    public ExitMessage(String name) {
        this.name = name;
    }

    public ExitMessage(String name, String message) {
        this.name = name;
        this.message = message;
    }
}
