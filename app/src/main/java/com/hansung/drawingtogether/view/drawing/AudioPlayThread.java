package com.hansung.drawingtogether.view.drawing;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.hansung.drawingtogether.data.remote.model.MyLog;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

// fixme jiyeon
@Getter
@Setter
public class AudioPlayThread implements Runnable {

    // Output Settings
    private int sampleRate = 5000;
    private int channelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferUnit = 2500; // 기본 단위 (0.25초마다)

    private int bufferSize; // fixme jiyeon[0428]

    private AudioTrack audioTrack;

    private ArrayList<byte[]> buffer = new ArrayList<>(5); // Audio Queue
    private String name;

    private boolean flag = false;
    private boolean start = false;

    @Override
    public void run() {
//        audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelCount, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
        audioTrack = new AudioTrack.Builder() // fixme jiyeon
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelCount)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .build();
        audioTrack.play();

        while(flag) {
            if (!start) { // fixme jiyeon[0428] - 처음에만 기다림
                synchronized (buffer) {
                    if (buffer.size() == 2) {
                        audioTrack.write(buffer.get(0), 0, bufferSize);
                        buffer.remove(0);
                        start = true;
                        MyLog.e("Audio", name + " Audio Start");
                    }
                }
            } else {
                synchronized (buffer) {
                    if (buffer.size() > 0) { // fixme jiyeon[0428] - 기다리지 않고 바로 출력
                        MyLog.e("Audio", name + " Buffer Size : " + buffer.size() + " : 1");
                        audioTrack.write(buffer.get(0), 0, bufferSize);
                        MyLog.e("Audio", name + " Buffer Size : " + buffer.size() + " : 2");
                        buffer.remove(0);
                        MyLog.e("Audio", name + " Buffer Size : " + buffer.size() + " : 3");
                    }
                }
            }
        }

        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;

        start = false;
    }

    public void setBufferUnitSize(int n) {
        bufferSize = bufferUnit * n;
    }
}
