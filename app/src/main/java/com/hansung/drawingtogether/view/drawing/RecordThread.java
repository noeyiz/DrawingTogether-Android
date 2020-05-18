package com.hansung.drawingtogether.view.drawing;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.view.main.AudioMessage;

import lombok.Setter;

//fixme jiyeon[0428]
@Setter
public class RecordThread implements Runnable {

    private int audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
    private int sampleRate = 5000;
    private int channelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferUnit = 2500;

    private int bufferSize;

    private AudioRecord audioRecord;
    private AcousticEchoCanceler acousticEchoCanceler;

    private MQTTClient mqttClient = MQTTClient.getInstance();
    private boolean flag = false;


    private byte[] readData;

    @Override
    public void run() {
        readData = new byte[bufferSize];

        audioRecord = new AudioRecord.Builder()
                .setAudioSource(audioSource)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelCount)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .build();

        initEchoCanceler(audioRecord.getAudioSessionId());
        audioRecord.startRecording();

        while(flag) {
            int ret = audioRecord.read(readData, 0, bufferSize);

            AudioMessage audioMessage = new AudioMessage(mqttClient.getMyName(), readData);
            MqttMessageFormat messageFormat = new MqttMessageFormat(audioMessage);

            mqttClient.publish(mqttClient.getTopic() + "_audio", JSONParser.getInstance().jsonWrite(messageFormat));
        }

        audioRecord.stop();
        audioRecord.release();

        releaseEchoCanceler();
    }

    public void setBufferUnitSize(int n) {
        bufferSize = bufferUnit * n;
    }


    // Echo Canceler
    public void initEchoCanceler(int audioSession) {
        acousticEchoCanceler = AcousticEchoCanceler.create(audioSession);
        acousticEchoCanceler.setEnabled(true);
    }

    public void releaseEchoCanceler() {
        acousticEchoCanceler.setEnabled(false);
        acousticEchoCanceler.release();
    }
}
