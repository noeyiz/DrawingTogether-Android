package com.hansung.drawingtogether.tester;

import android.os.Environment;
import android.util.Log;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;

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

    /* 측정한 성능 데이터를 csv 파일 형식으로 저장하는 함수 */
    public void receiveTimeWrite() {

        MQTTClient.getInstance().getProgressDialog().show();

        String data = "";
        Vector<PerformanceData> times = MQTTClient.receiveTimeList;

        // 파일 이름: "MsgReceiveTime_화면스트로크개수_전송스트로크개수_전송스트로크세그먼트개수"
        String file = "MsgReceiveTime_" + DrawingTester.getInstance().getBackground() + "_"
                + DrawingTester.getInstance().getSending() + "_" + DrawingTester.getInstance().getSSegment() + ".csv";

        for(int i=0; i<MQTTClient.receiveTimeList.size(); i++)
            data += MQTTClient.receiveTimeList.get(i).toString();


        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/performance1222"); // 저장 경로

        Log.i("tester", "performance data save path = " + dir);
        Log.i("tester", "performance data file name = " + file);

        // 폴더 생성
        if(!dir.exists()){ // 폴더 없을 경우
            dir.mkdir(); // 폴더 생성
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(dir + "/"+ file, true));
            buf.append(data); // 파일 쓰기
            buf.newLine(); // 개행
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
