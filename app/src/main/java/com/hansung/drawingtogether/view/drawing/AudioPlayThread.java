package com.hansung.drawingtogether.view.drawing;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.hansung.drawingtogether.data.remote.model.MyLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

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
    private String userName;

    private boolean flag = false;

    @Override
    public void run() {
        // 에코 캔슬링 기능이 있는 VOICE_COMMUNICATION 사용하도록 수정
        // audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelCount, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
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
        MyLog.i("audio", "Start Playing");

        try {
            while(true) {
                if (!flag) {
                    synchronized (audioTrack) {
                        MyLog.i("audio", "Audio Track Wait");
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
            MyLog.i("audio", "Play Thread is dead");
        }
    }

    // Play Thread 생성할 때 오디오 블록을 담을 오디오 버퍼의 사이즈 설정
    public void setBufferUnitSize(int n) {
        bufferSize = bufferUnit * n;
    }

    // Exit Or Close 할 때 Audio Track 리소스 해제해주기 위함
    public void stopPlaying() {
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
        MyLog.i("audio", "Stop Playing");
    }
}
