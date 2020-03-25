package com.hansung.drawingtogether.data.remote.model;


import android.os.Environment;
import android.util.Log;

import com.hansung.drawingtogether.view.drawing.DrawingEditor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import lombok.Getter;

// Logger Class
@Getter
public enum Logger {
    INSTANCE;

    private DrawingEditor de = DrawingEditor.getInstance();

    private String log = "";
    private String uncaughtException = "";


    public static Logger getInstance() { return INSTANCE; }

    public void info(String message) { log += "INFO : " + new LogContent(message).toString() + "\n"; }

    public void warn(String message) { log += "WARN : " + new LogContent(message).toString() + "\n"; }

    public void error(String message) { log += "ERROR : " + new LogContent(message).toString() + "\n"; }

    public void loggingUncaughtException(Thread thread, StackTraceElement[] ste) {

        uncaughtException += "\n\n" + "[ Thread Name ]\n" + thread.getName() + "\n\n";

        uncaughtException += "[ Print Stack Trace ]\n";
        for(int i=0; i< ste.length; i++)
            uncaughtException += ste[i].toString() + "\n";

    }


    public File createLogFile() {

        File logFile = new File(Environment.getExternalStorageDirectory() + File.separator + de.getMyUsername() + ".log");
        Log.e("file", Environment.getExternalStorageDirectory() + File.separator + de.getMyUsername() + ".log");

        try {
           FileWriter fw = new FileWriter(logFile);
           fw.write(log + uncaughtException);

           fw.close();

           return logFile;

        } catch (IOException io) { io.printStackTrace(); }

        return null;
    }

    public void deleteLogFile(File file) {

        if(file.exists()) file.delete();

    }

}
