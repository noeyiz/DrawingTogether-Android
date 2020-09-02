package com.hansung.drawingtogether.view.drawing;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.hansung.drawingtogether.data.remote.model.MyLog;

import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

// fixme jiyeon
@Getter
@Setter
public class AudioPlayThread extends Thread {

    // Output Settings
    private int sampleRate = 5000;
    private int channelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferUnit = 2500; // 기본 단위 (0.25초마다)

    private int bufferSize;

    private AudioTrack audioTrack;

    private List<byte[]> buffer = Collections.synchronizedList(new ArrayList<byte[]>(5));
//    private ArrayList<byte[]> buffer = new ArrayList<>(5); // Audio Queue
    private String userName;

    private boolean flag = false;

    @Override
    public void run() {
//        audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelCount, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
        audioTrack = new AudioTrack.Builder()
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
        MyLog.e("Audio", "Start Playing");

        try {
            while(true) {
                if (!flag) {
                    synchronized (audioTrack) {
                        MyLog.e("Audio", "Audio Track Wait");
                        audioTrack.wait();
                    }
                    flag = true;
                }

                synchronized (buffer) {
                    if (buffer.size() > 0) {
                        audioTrack.write(buffer.get(0), 0, bufferSize);
                        buffer.remove(0);
                    }
                }
            }
        } catch (InterruptedException e) {
            MyLog.e("Audio", "Play Thread is dead");
        }
    }

    public void setBufferUnitSize(int n) {
        bufferSize = bufferUnit * n;
    }

    public void stopPlaying() {
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
        MyLog.e("Audio", "Stop Playing");
    }
}
