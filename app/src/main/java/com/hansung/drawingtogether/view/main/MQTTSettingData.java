package com.hansung.drawingtogether.view.main;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// fixme hyeyeon - mainviewmodel에서 drawingviewmodel로 넘겨줄 데이터들
public class MQTTSettingData {
    private static final MQTTSettingData ourInstance = new MQTTSettingData();
    private String ip;
    private String port;
    private String topic;
    private String name;
    private String password;
    private boolean master;

    public static MQTTSettingData getInstance() {
        return ourInstance;
    }

    private MQTTSettingData() {
    }
}
