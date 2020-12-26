package com.hansung.drawingtogether.tester;

import android.os.Environment;
import android.util.Log;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.view.drawing.DrawingEditor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public enum PerformanceDataWriter {
    INSTANCE;

    private static String PROPAGATION_TAG = "propagation";
    private static String DRAWING_TAG = "drawing";

    public void write() {
        write(MQTTClient.propagationTimeList, PROPAGATION_TAG);
        write(MQTTClient.drawingTimeList, DRAWING_TAG);
    }

    /* 측정한 성능 데이터를 csv 파일 형식으로 저장하는 함수 */
    private void write(Vector<PerformanceData> timeList, String tag) {
        // 파일 이름: "propagation_화면스트로크개수"
        // 파일 이름: "propagation_참가자수" (중간 참가자가 참가하기 전에 있던 참가자 수)
        String file = tag + "_" + DrawingEditor.getInstance().getDrawingComponents().size() + ".csv";
        //String file = tag + "_" + (MQTTClient.getInstance().getUserList().size() - 1) + ".csv";

        String data = "";

        if(tag.equals(PROPAGATION_TAG)) {
            for (int i = 0; i < timeList.size(); i++)
                data += timeList.get(i).toStringPropagation();
        }
        else if(tag.equals(DRAWING_TAG)) {
            for (int i = 0; i < timeList.size(); i++)
                data += timeList.get(i).toStringDrawing();
        }


        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/chart3(2)"); // 저장 경로

        Log.i("tester", "performance data save path = " + dir);
        Log.i("tester", "performance data file name = " + file);

        // 폴더 생성
        if(!dir.exists()){ // 폴더 없을 경우
            dir.mkdir(); // 폴더 생성
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(dir + "/"+ file, true));
            buf.append(data); // 파일 쓰기
            buf.close();
        } catch (FileNotFoundException fnfe) {
            Log.e("tester", "data save file not found exception!");
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            Log.e("tester", "data save file io exception!");
            ioe.printStackTrace();
        } finally {
            MQTTClient.getInstance().getProgressDialog().dismiss();
        }
    }



    public static PerformanceDataWriter getInstance() { return INSTANCE; }
}