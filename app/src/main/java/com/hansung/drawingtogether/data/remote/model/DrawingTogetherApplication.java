package com.hansung.drawingtogether.data.remote.model;

import android.app.Application;


public class DrawingTogetherApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("application", "DrawingTogether App is loaded");

        // fixme nayen 애플리케이션 비정상 종료( 오류 발생 ) 시 처리되는 핸들러
        AbnormalTerminationHandler ath = new AbnormalTerminationHandler();
        Thread.setDefaultUncaughtExceptionHandler(ath);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e("application", "DrawingTogether App terminate");


    }
}
