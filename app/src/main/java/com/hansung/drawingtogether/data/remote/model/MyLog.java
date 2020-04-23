package com.hansung.drawingtogether.data.remote.model;


public final class MyLog {

    private static final Boolean DEBUG = true; // todo DebugConfigure
    private static Logger logger = Logger.getInstance();

    public static void i(String tag, String msg) {
        // if(BuildConfig.DEBUG)
        if(DEBUG)
            android.util.Log.i(tag, msg);
        // logger.info(tag, msg);
    }

    public static void d(String tag, String msg) {
        // if(BuildConfig.DEBUG)
        if(DEBUG)
            android.util.Log.d(tag, msg);
        // logger.debug(tag, msg);
    }

    public static void e(String tag, String msg) {
        // if(BuildConfig.DEBUG)
        if(DEBUG)
            android.util.Log.e(tag, msg);
        logger.error(tag, msg);
    }

    public static void v(String tag, String msg) {
        // if(BuildConfig.DEBUG)
        if(DEBUG)
            android.util.Log.v(tag, msg);

        // logger.verbose(tag, msg);
    }

    public static void w(String tag, String msg) {
        // if(BuildConfig.DEBUG)
        if(DEBUG)
            android.util.Log.w(tag, msg);

        // logger.warn(tag, msg);
    }

}
