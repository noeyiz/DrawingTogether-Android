package com.hansung.drawingtogether.view.main;

import lombok.Getter;

@Getter

/* 자신이 회의방에 새로 참가하였음을 알리는 JoinMessage */
public class JoinMessage {

    private String name;

    public JoinMessage(String name) { this.name = name; }

}