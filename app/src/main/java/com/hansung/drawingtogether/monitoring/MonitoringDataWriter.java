package com.hansung.drawingtogether.monitoring;

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

/* 측정된 속도 데이터를 파일에 기록하는 클래스 */
/* 측정된 데이터를 확인하기 위한 .txt 파일과 차트를 그리기 위한 .csv 파일을 저장 */
/* 기록되는 데이터 */
/* MQTTClient.receiveTimeList, MQTTClient.displayTimeList, MQTTClient.deliveryTimeList */
public enum MonitoringDataWriter {
    INSTANCE;

    public static MonitoringDataWriter getInstance() {
        return INSTANCE;
    }

    public void write() {
        receiveTimeWrite("csv");
        displayTimeWrite("csv");
        deliveryTimeWrite("csv");

        receiveTimeWrite("txt");
        displayTimeWrite("txt");
        deliveryTimeWrite("txt");
    }

    public void receiveTimeWrite(String extension) {
        String data = "";
        int number = -1;
        Vector<Velocity> times = MQTTClient.receiveTimeList;

        switch(extension) {
            case "csv":

                for(int i=0; i<MQTTClient.receiveTimeList.size(); i++) {
                    if(MQTTClient.receiveTimeList.get(i).getEnd() != 0) {
                        data += MQTTClient.receiveTimeList.get(i).getTime() + "\n";
                    }
                }

                break;

            case "txt":

                for(int i=0; i<MQTTClient.receiveTimeList.size(); i++) {
                    if(MQTTClient.receiveTimeList.get(i).getEnd() != 0) {
                        data += "[start=" + MQTTClient.receiveTimeList.get(i).getStart()
                                + ", end=" + MQTTClient.receiveTimeList.get(i).getEnd()
                                + ", time=" + MQTTClient.receiveTimeList.get(i).getTime() + "]";
                    }
                }
                break;
        }

        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/performance"); // 저장 경로

        Log.e("monitoring", "performance data save path = " + dir);

        // 폴더 생성
        if(!dir.exists()){ // 폴더 없을 경우
            dir.mkdir(); // 폴더 생성
        }
        try {
            long now = System.currentTimeMillis(); // 현재시간 받아오기
            Date date = new Date(now); // Date 객체 생성
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String nowTime = sdf.format(date) + "\n";

            BufferedWriter buf = new BufferedWriter(new FileWriter(dir + "/receive_time"+ "." + extension, true));
            buf.append(nowTime + " "); // 날짜 쓰기
            buf.append(data); // 파일 쓰기
            buf.newLine(); // 개행
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void displayTimeWrite(String extension) {

        String data = "";
        int number = -1;
        Vector<Velocity> times = MQTTClient.displayTimeList;

        switch(extension) {
            case "csv":

                for(int i=0; i<MQTTClient.displayTimeList.size(); i++) {
                    if(MQTTClient.displayTimeList.get(i).getEnd() != 0) {
                        data +=  MQTTClient.displayTimeList.get(i).getTime() + "\n";
                    }
                }

                break;

            case "txt":

                for(int i=0; i<MQTTClient.displayTimeList.size(); i++) {
                    if(MQTTClient.displayTimeList.get(i).getEnd() != 0) {
                        data += "[start=" + MQTTClient.displayTimeList.get(i).getStart()
                                + ", end=" + MQTTClient.displayTimeList.get(i).getEnd()
                                + ", time=" + MQTTClient.displayTimeList.get(i).getTime() + "]";
                    }
                }
                break;
        }

        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/performance"); // 저장 경로

        Log.e("monitoring", "performance data save path = " + dir);

        // 폴더 생성
        if(!dir.exists()){ // 폴더 없을 경우
            dir.mkdir(); // 폴더 생성
        }
        try {
            long now = System.currentTimeMillis(); // 현재시간 받아오기
            Date date = new Date(now); // Date 객체 생성
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String nowTime = sdf.format(date) + "\n";

            BufferedWriter buf = new BufferedWriter(new FileWriter(dir + "/display_time"+ "." + extension, true));
            buf.append(nowTime + " "); // 날짜 쓰기
            buf.append(data); // 파일 쓰기
            buf.newLine(); // 개행
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deliveryTimeWrite(String extension) {
        String data = "";
        int number = -1;
        Vector<Velocity> times = MQTTClient.deliveryTimeList;

        switch(extension) {
            case "csv":

                for(int i=0; i<MQTTClient.deliveryTimeList.size(); i++) {
                    if(MQTTClient.deliveryTimeList.get(i).getEnd() != 0) {
                        number++;
                        data +=  MQTTClient.deliveryTimeList.get(i).getComponent() + "," + MQTTClient.deliveryTimeList.get(i).getTime() + "\n";
                    }
                }

                break;

            case "txt":

                for(int i=0; i<MQTTClient.deliveryTimeList.size(); i++) {
                    if(MQTTClient.deliveryTimeList.get(i).getEnd() != 0) {
                        data += "[component=" + MQTTClient.deliveryTimeList.get(i).getComponent()
                                + ", time=" + MQTTClient.deliveryTimeList.get(i).getTime() + "]";
                    }
                }
                break;
        }

        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/performance"); // 저장 경로

        Log.e("monitoring", "performance data save path = " + dir);

        // 폴더 생성
        if(!dir.exists()){ // 폴더 없을 경우
            dir.mkdir(); // 폴더 생성
        }
        try {
            long now = System.currentTimeMillis(); // 현재시간 받아오기
            Date date = new Date(now); // Date 객체 생성
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String nowTime = sdf.format(date) + "\n";

            BufferedWriter buf = new BufferedWriter(new FileWriter(dir + "/delivery_time"+ "." + extension, true));
            buf.append(nowTime + " "); // 날짜 쓰기
            buf.append(data); // 파일 쓰기
            buf.newLine(); // 개행
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
