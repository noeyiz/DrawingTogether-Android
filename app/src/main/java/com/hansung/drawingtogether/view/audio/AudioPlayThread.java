package com.hansung.drawingtogether.view.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

import com.hansung.drawingtogether.data.remote.model.Log; // fixme nayeon

// fixme jiyeon
@Getter
@Setter
public class AudioPlayThread implements Runnable {

    private int sampleRate = 5000;
    private int channelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferUnit = 2500; // 기본 단위 (0.25초마다)
    private int bufferSize = 5000;

    private AudioTrack audioTrack;

    private ArrayList<byte[]> buffer = new ArrayList<>(5);
    private String name;

    private boolean flag = false;

    @Override
    public void run() {
        audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelCount, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
        audioTrack.play();

        while(flag) {
            synchronized (buffer) {
                if (buffer.size() == 2) {
                    Log.e("2yeonz", buffer.size() + " : 1" + name);
                    audioTrack.write(buffer.get(0), 0, bufferSize);
                    Log.e("2yeonz", buffer.size() + " : 2" + name);
                    buffer.remove(0);
                    Log.e("2yeonz", buffer.size() + " : 3" + name);
                }
            }
        }

        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
    }

    public void setBufferUnitSize(int n) {
        bufferSize = bufferUnit * n;
    }
}
