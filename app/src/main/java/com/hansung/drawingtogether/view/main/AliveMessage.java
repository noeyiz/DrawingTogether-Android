package com.hansung.drawingtogether.view.main;

import lombok.Getter;

@Getter

/* 회의방에 참가 중임을 지속적으로 알리는 Alive Message */
public class AliveMessage {

    private String name;

    public AliveMessage(String name) { this.name = name; }

}
