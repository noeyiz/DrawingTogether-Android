package com.hansung.drawingtogether.view.drawing;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;

import lombok.Setter;

//fixme jiyeon[0821]
@Setter
public class RecordThread implements Runnable {

    // Input Settings
    private int audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
    private int sampleRate = 5000;
    private int channelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferUnit = 2500;

    private int bufferSize;

    private AudioRecord audioRecord;

    private MQTTClient mqttClient = MQTTClient.getInstance();
    byte[] nameByte = mqttClient.getMyName().getBytes();

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

        audioRecord.startRecording();

        while(flag) {
            int ret = audioRecord.read(readData, 0, bufferSize);

            publishAudioMessage(readData);
//            AudioMessage audioMessage = new AudioMessage(mqttClient.getMyName(), readData);
//            MqttMessageFormat messageFormat = new MqttMessageFormat(audioMessage);
//            mqttClient.publish(mqttClient.getTopic() + "_audio", JSONParser.getInstance().jsonWrite(messageFormat));
        }

        audioRecord.stop();
        audioRecord.release();
    }

    public void setBufferUnitSize(int n) {
        bufferSize = bufferUnit * n;
    }

    // fixme jiyeon - 오디오 데이터 + 이름 (바이너리 데이터 자체를 보내도록 변경)
    public void publishAudioMessage(byte[] audioData) {
        byte[] audioMessage = new byte[audioData.length + nameByte.length];

        System.arraycopy(audioData, 0, audioMessage, 0, audioData.length);
        System.arraycopy(nameByte, 0, audioMessage, audioData.length, nameByte.length);

        mqttClient.publish(mqttClient.getTopic_audio(), audioMessage);
    }

}


