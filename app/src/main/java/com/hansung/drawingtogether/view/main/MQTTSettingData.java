package com.hansung.drawingtogether.view.main;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/* MQTTClient init에 필요한 데이터들 */
public class MQTTSettingData {

    private static final MQTTSettingData data = new MQTTSettingData();
    private String ip;
    private String port;
    private String topic;
    private String name;
    private String password;
    private String masterName;
    private boolean master;

    public static MQTTSettingData getInstance() { return data; }

}
