package com.hansung.drawingtogether.view.drawing;

import com.hansung.drawingtogether.data.remote.model.MyLog;

import java.util.concurrent.ThreadFactory;

public class LowPriorityThreadFactory implements ThreadFactory {
    private int count = 1;
    private String TAG = "LowPriorityThreadFactory";

    @Override
    public Thread newThread(Runnable r) {
        MyLog.i("thread", "new thread " + count);

        Thread t = new Thread(r);
        t.setName("LowPriority " + count++);
        t.setPriority(4);
        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable throwable) {
                MyLog.d(TAG, "Thread = " +t.getName() + ", error = " + throwable.getMessage());
            }
        });
        return t;
    }

}