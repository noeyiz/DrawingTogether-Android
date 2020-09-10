package com.hansung.drawingtogether.monitoring;

import android.util.Log;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.view.drawing.JSONParser;
import com.hansung.drawingtogether.view.drawing.MqttMessageFormat;

import lombok.Getter;

@Getter
public enum MonitoringRunnable implements Runnable {
    INSTANCE;

    private MQTTClient client = MQTTClient.getInstance();

    private int second = 3000;

    public static MonitoringRunnable getInstance() { return INSTANCE; }


    @Override
    public void run() {
        String topic_monitoring = client.getTopic_monitoring();  // 복사

        while (true) {
            try {

                MqttMessageFormat mqttMessageFormat = new MqttMessageFormat(client.getComponentCount());

                Log.e("monitoring", "mqtt message format\n" + JSONParser.getInstance().jsonWrite(mqttMessageFormat));

                client.publish(topic_monitoring, JSONParser.getInstance().jsonWrite(mqttMessageFormat));

                Log.e("monitoring", "publish monitoring message");

                Thread.sleep(second);

            } catch (InterruptedException e) {
                Log.e("monitoring", "monitoring thread is dead");
                break;
            }

        }
    }
}
