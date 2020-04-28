package com.hansung.drawingtogether.view.drawing;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.view.main.AudioMessage;

import lombok.Setter;

//fixme jiyeon
@Setter
public class RecordThread implements Runnable {

    private int audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
    private int sampleRate = 5000;
    private int channelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferUnit = 2500;
    private int bufferSize = 5000;

    private AudioRecord audioRecord; // fixme jiyeon

    private MQTTClient mqttClient = MQTTClient.getInstance();
    private boolean flag = false;

    private byte[] readData = new byte[bufferSize];

    @Override
    public void run() {
        // todo permission 생각해야 함
        audioRecord = new AudioRecord.Builder() // fixme jiyeon
                .setAudioSource(audioSource)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelCount)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .build();
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