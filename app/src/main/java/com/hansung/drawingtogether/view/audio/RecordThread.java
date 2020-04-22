package com.hansung.drawingtogether.view.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.view.drawing.JSONParser;
import com.hansung.drawingtogether.view.drawing.MqttMessageFormat;
import com.hansung.drawingtogether.view.main.AudioMessage;
import com.hansung.drawingtogether.view.main.DeleteMessage;

import org.eclipse.paho.client.mqttv3.MqttClient;

import lombok.Setter;

//fixme jiyeon
@Setter
public class RecordThread implements Runnable {

    private int audioSource = MediaRecorder.AudioSource.MIC;
    private int sampleRate = 5000;
    private int channelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferUnit = 2500;
    private int bufferSize = 5000;

    private AudioRecord audioRecord = new AudioRecord(audioSource, sampleRate, channelCount, audioFormat, bufferSize);

    private MQTTClient mqttClient = MQTTClient.getInstance();
    private boolean flag = false;

    private byte[] readData = new byte[bufferSize];

    @Override
    public void run() {
        // todo permission 생각해야 함
        audioRecord.startRecording();

        while(flag) {
            int ret = audioRecord.read(readData, 0, bufferSize);

            AudioMessage audioMessage = new AudioMessage(mqttClient.getMyName(), readData);
            MqttMessageFormat messageFormat = new MqttMessageFormat(audioMessage);

            mqttClient.publish(mqttClient.getTopic() + "_audio", JSONParser.getInstance().jsonWrite(messageFormat));
        }

        audioRecord.stop();
        audioRecord.release();
    }

    public void setBufferUnitSize(int n) {
        bufferSize = bufferUnit * n;
    }
}
