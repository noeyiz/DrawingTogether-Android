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

        try {
            Log.i("monitoring", "wait... topic record save");
            Thread.sleep(second);
            Log.i("monitoring", "enable monitoring thread!!");
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        while (true) {
            try {

                MqttMessageFormat mqttMessageFormat = new MqttMessageFormat(client.getComponentCount());

                Log.i("monitoring", "mqtt message format\n" + JSONParser.getInstance().jsonWrite(mqttMessageFormat));

                client.publish(topic_monitoring, JSONParser.getInstance().jsonWrite(mqttMessageFormat));

                Thread.sleep(second);

            } catch (InterruptedException e) {
                Log.e("monitoring", "monitoring thread is dead");
                break;
            }

        }

    }
}
