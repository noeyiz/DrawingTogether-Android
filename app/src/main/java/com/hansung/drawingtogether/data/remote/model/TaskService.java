package com.hansung.drawingtogether.data.remote.model;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/* Task가 종료되는 시점을 감지하기 위한 클래스 */
public class TaskService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i("TaskService", "Task Service Removed");
        stopSelf();
    }
}
