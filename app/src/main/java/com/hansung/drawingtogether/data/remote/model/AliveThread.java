package com.hansung.drawingtogether.data.remote.model;


import android.util.Log;

import com.hansung.drawingtogether.view.drawing.DrawingViewModel;
import com.hansung.drawingtogether.view.drawing.JSONParser;
import com.hansung.drawingtogether.view.drawing.MqttMessageFormat;
import com.hansung.drawingtogether.view.main.AliveMessage;

import lombok.Getter;

@Getter

/* T초에 한번씩 Alive Message Publish */
public enum AliveThread implements Runnable {
    INSTANCE;

    private MQTTClient client = MQTTClient.getInstance();

    private DrawingViewModel drawingViewModel;
    private int second = 10000;
    public static AliveThread getInstance() {
        return INSTANCE;
    }

    @Override
    public void run() {
        String topic_alive = client.getTopic_alive();
        String myName = client.getMyName();

        drawingViewModel = client.getDrawingViewModel();
        while (true) {

            try {
                /* Alive Message Publish */
                AliveMessage aliveMessage = new AliveMessage(myName);
                MqttMessageFormat mqttMessageFormat = new MqttMessageFormat(aliveMessage);
                client.publish(topic_alive, JSONParser.getInstance().jsonWrite(mqttMessageFormat));

                Thread.sleep(second);

            } catch (InterruptedException e) {
                MyLog.i("Alive Thread", "Alive Thread is dead");
                break;
            }

        }
    }

    /* SETTER */
    public void setSecond(int second) { this.second = second; }
}
