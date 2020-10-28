package com.hansung.drawingtogether.view.drawing;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.hansung.drawingtogether.data.remote.model.MyLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AudioPlayThread { //extends Thread {
    private ExecutorService executor;

    /* Output Settings */
    private int sampleRate = 5000;
    private int channelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferUnit = 2500; // 기본 단위 (0.25초마다)
    private int bufferSize;

    /* 오디오 블록을 출력하는 객체 */
    private AudioTrack audioTrack;

    /* 오디오 큐 */
    private List<byte[]> buffer = Collections.synchronizedList(new ArrayList<byte[]>(5));

    private String userName;
    private boolean flag = false;

    public AudioPlayThread() {
        if(executor == null) {
            MyLog.i("thread", "new audio play thread executor");
            executor  = Executors.newFixedThreadPool(10, new LowPriorityThreadFactory());
        }
    }

    public void start() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                /* 에코 캔슬링 기능이 있는 VOICE_COMMUNICATION 사용하도록 수정 */
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
                MyLog.i("Audio", "Start Playing");

                try {
                    while(true) {
//                        if (buffer.size() >= 5) {
//                            synchronized (buffer) {
//                                buffer.clear();
//                                MyLog.i("Audio", "Buffer Clear ...");
//                            }
//                        }

                        //MyLog.i("Audio", "Buffer Size : " + buffer.size());

                        if (!flag) {
                            synchronized (audioTrack) {
                                MyLog.i("Audio", "Audio Track Wait");
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
                    MyLog.i("Audio", "Play Thread is dead");
                }
            }
        });
    }

    /* Play Thread 생성할 때 오디오 블록을 담을 오디오 버퍼의 사이즈 설정 */
    public void setBufferUnitSize(int n) {
        bufferSize = bufferUnit * n;
    }

    /* Exit Or Close할 때 Audio Track 리소스 해제해주기 위함  */
    public void stopPlaying() {
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
        MyLog.i("Audio", "Stop Playing");
    }
}
