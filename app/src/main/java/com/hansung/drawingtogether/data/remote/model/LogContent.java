package com.hansung.drawingtogether.data.remote.model;

import com.hansung.drawingtogether.view.drawing.DrawingEditor;

import java.text.SimpleDateFormat;
import java.util.Date;

// 로그에 기록할 정보에 대한 클래스
public class LogContent {

    String time; // 시간
    String username; // 사용자 이름
    String message; // 로그 메시지

    public LogContent(String message) {
        this.time = getTime(System.currentTimeMillis());
        this.username = DrawingEditor.getInstance().getMyUsername();
        this.message = message;
    }


    @Override
    public String toString() {
        return "[ " + time + " ] " + username + " : " + message;
    }

    public String getTime(long time) {
        Date date = new Date(time);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        return formatter.format(date);
    }

}
