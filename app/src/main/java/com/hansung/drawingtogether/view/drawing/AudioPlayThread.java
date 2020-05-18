package com.hansung.drawingtogether.view.drawing;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

// fixme jiyeon
@Getter
@Setter
public class AudioPlayThread implements Runnable {

    private int sampleRate = 5000;
    private int channelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferUnit = 2500; // 기본 단위 (0.25초마다)
    private int bufferSize; // fixme jiyeon[0428]

    private AudioTrack audioTrack;

    private ArrayList<byte[]> buffer = new ArrayList<>(5);
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
                        Log.e("2yeonz", "2라서 출력함");
                    }
                }
            } else {
                synchronized (buffer) {
                    if (buffer.size() > 0) { // fixme jiyeon[0428] - 기다리지 않고 바로 출력
                        Log.e("2yeonz", buffer.size() + " : 1 " + name);
                        audioTrack.write(buffer.get(0), 0, bufferSize);
                        Log.e("2yeonz", buffer.size() + " : 2 " + name);
                        buffer.remove(0);
                        Log.e("2yeonz", buffer.size() + " : 3 " + name);
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
