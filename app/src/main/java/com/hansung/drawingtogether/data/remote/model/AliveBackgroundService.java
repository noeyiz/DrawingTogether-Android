package com.hansung.drawingtogether.data.remote.model;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class AliveBackgroundService extends Service {

    private MQTTClient client = MQTTClient.getInstance();
    private AliveThread aliveThread;
    private Thread thread;

    @Override
    public void onCreate() {
        super.onCreate();
        aliveThread = AliveThread.getInstance();
        aliveThread.setSecond(2000);
        aliveThread.setCount(0);
        thread = new Thread(aliveThread);
        thread.start();
        client.setThread(thread);
        Log.e("alive", "AliveBackgroundService start, alive thread start");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread.interrupt();
        stopSelf();
        Log.e("alive", "AliveBackgroundService dead, alive thread dead");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

