package com.hansung.drawingtogether.data.remote.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

/* 멤버 */
public class User {

    private String name;  // 멤버의 이름
    private int count;  // 멤버의 Alive Count
    private int action;
    private boolean isInitialized;

    public User(String name, int count, int action, boolean isInitialized) {
        this.name = name;
        this.count = count;
        this.action = action;
        this.isInitialized = isInitialized;
    }

}
