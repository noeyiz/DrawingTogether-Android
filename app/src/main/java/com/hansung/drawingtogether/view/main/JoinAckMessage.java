package com.hansung.drawingtogether.view.main;

import lombok.Getter;

@Getter

/* 자신이 회의에 참여중인 멤버임을 알리는 joinAckMessage */
public class JoinAckMessage {

    private String name;
    private String target;

    public JoinAckMessage(String name, String target) {
        this.name = name;
        this.target = target;
    }

}
