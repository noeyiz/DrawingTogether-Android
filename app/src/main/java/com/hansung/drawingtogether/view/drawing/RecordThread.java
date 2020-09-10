package com.hansung.drawingtogether.view.drawing;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordThread extends Thread {

    /* Input Settings */
    private int audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
    private int sampleRate = 5000;
    private int channelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferUnit = 2500;
    private int bufferSize;

    /* 오디오 샘플을 캡쳐하는 객체 */
    private AudioRecord audioRecord;

    /* 캡쳐한 오디오 샘플들의 모음 (오디오 블록) */
    private byte[] readData;

    private MQTTClient mqttClient = MQTTClient.getInstance();

    byte[] nameByte = mqttClient.getMyName().getBytes();
    private boolean flag = false;

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
        MyLog.i("Audio", "Start Recording");

        try {
            while (true) {
                if (!flag) {
                    synchronized (audioRecord) {
                        MyLog.i("Audio", "RecordThread Wait");
                        audioRecord.wait();
                    }
                    flag = true;
                }

                int ret = audioRecord.read(readData, 0, bufferSize);

                publishAudioMessage(readData);
            }
        } catch (InterruptedException e) {
            MyLog.i("Audio", "Record Thread is dead");
        }
    }

    /* Record Thread 생성할 때 오디오 블록을 담을 오디오 버퍼의 사이즈 설정 */
    public void setBufferUnitSize(int n) {
        bufferSize = bufferUnit * n;
    }

    /* Exit Or Close할 때 AudioRecord 리소스 해제해주기 위함 */
    public void stopRecording() {
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        MyLog.i("Audio", "Stop Recording");
    }

    /* 오디오 데이터 + 이름 (바이너리 데이터 자체를 보내도록 변경) */
    public void publishAudioMessage(byte[] audioData) {
        byte[] audioMessage = new byte[audioData.length + nameByte.length];

        System.arraycopy(audioData, 0, audioMessage, 0, audioData.length);
        System.arraycopy(nameByte, 0, audioMessage, audioData.length, nameByte.length);

//        mqttClient.publish(mqttClient.getTopic_audio(), audioMessage);
    }

}


