package com.hansung.drawingtogether.data.remote.model;

import android.app.Application;


public class SeeSeeCallCallApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MyLog.e("application", "DrawingTogether App is loaded");

        // fixme - 애플리케이션 비정상 종료( 오류 발생 ) 시 처리되는 핸들러
        AbnormalTerminationHandler ath = new AbnormalTerminationHandler();
        Thread.setDefaultUncaughtExceptionHandler(ath);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        MyLog.e("application", "DrawingTogether App terminate");


    }
}
