package com.hansung.drawingtogether.data.remote.model;


/* 비 정상 종료 시 실행될 핸들러 */
public class AbnormalTerminationHandler
        implements Thread.UncaughtExceptionHandler {

    private Logger logger = Logger.getInstance();

    @Override
    public void uncaughtException(Thread thread, Throwable e) {
        MyLog.e("Exception", "UncaughtException");

        logger.loggingUncaughtException(thread, e.getStackTrace()); // 발생한 오류에 대한 메시지 로그에 기록
        logger.uploadLogFile(ExitType.ABNORMAL);

    }
}
