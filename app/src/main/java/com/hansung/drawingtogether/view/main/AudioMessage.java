package com.hansung.drawingtogether.view.main;

import lombok.Getter;

// fixme jiyeon
@Getter
public class AudioMessage {
    private String name;
    private byte[] data; // 오디오 데이터

    public AudioMessage(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }
}
