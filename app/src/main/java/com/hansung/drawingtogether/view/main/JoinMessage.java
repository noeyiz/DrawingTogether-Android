package com.hansung.drawingtogether.view.main;

import com.hansung.drawingtogether.data.remote.model.User;

import java.util.List;

import lombok.Getter;

@Getter
public class JoinMessage {
    String master; // 마스터 이름 (master:"이름")
    String name; // 사용자 이름 (name:"이름")

    String to; // 중간자 이름 (to:"이름") - 마스터가 알아낸 사용자 리스트에서 마지막번째 사람의 이름
    List<User> userList; // 사용자 리스트  // fixme hyeyeon
    //String loadingData; // 데이터를 받을 토픽 (topic_data)

    float drawnCanvasWidth;
    float drawnCanvasHeight;

    public JoinMessage(String master, String to, List<User> userList) { // "master":"이름"/"userList":"이름1,이름2,이름3"/"loadingData":"..."
        this.master = master;
        this.to = to;
        this.userList = userList;
        //this.loadingData = loadingData;
    }

    public JoinMessage(String name, float drawnCanvasWidth, float drawnCanvasHeight) {
        this.name = name;
        this.drawnCanvasWidth = drawnCanvasWidth;
        this.drawnCanvasHeight = drawnCanvasHeight;
    } // "name":"이름"
}