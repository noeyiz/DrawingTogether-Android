package com.hansung.drawingtogether.view.main;

import lombok.Getter;

@Getter

/* 회의방 종료 시 전송하는 CloseMessage (마스터만이 Close Message 전송) */
public class CloseMessage {

    private String name;

    public CloseMessage(String name) { this.name = name; }

}
