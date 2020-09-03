package com.hansung.drawingtogether.view.main;

import lombok.Getter;

@Getter

/* 회의방에서 나갈 떄 다른 참가자에게 전송하는 ExitMessage */
public class ExitMessage {

    private String name;

    public ExitMessage(String name) { this.name = name; }

}
