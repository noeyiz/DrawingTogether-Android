package com.hansung.drawingtogether.data.remote.model;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class TaskService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("task", "task removed");
        // 최근에 실행한 앱에서 지울 때

        stopSelf();
    }
}
