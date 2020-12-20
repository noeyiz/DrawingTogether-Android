package com.hansung.drawingtogether.tester;

import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.Getter;

@Getter
public class Velocity {
    private long start;
    private long end;
    private double time;

    private String date;
    private SimpleDateFormat df = new SimpleDateFormat("HH:mm");

    private int component;

    private int size;

    private String participant; // 중간 참여자 이름

    public Velocity(long start) { // 메시지 수신 시간 측정 시 사용하는 생성자
        this.start = start;
    }

    public Velocity(long start, int component, int size) { // 화면에 그리는 시간 측정 시 사용하는 생성자
        this.start = start;
        this.component = component;
        this.size = size;
    }

    public Velocity(long start, String participant, int size, int component) { // 중간 참여자에게 보낸 메시지 수신 시간 측정 시 사용하는 생성자
        this.start = start;
        this.participant = participant;
        this.size = size;
        this.component = component;
        this.date = df.format(new Date());
    }

    public void calcTime(long end, int size) {
        this.end = end;
        this.time = ( this.end - this.start )/1000.0;
        this.size = size;
        this.date = df.format(new Date());
    }

    public void calcTime(long end) {
        this.end = end;
        this.time = ( this.end - this.start )/1000.0;
        this.date = df.format(new Date());
    }

    @Override
    public String toString() {
        return "Velocity{" +
                "start=" + start +
                ", end=" + end +
                ", time=" + time +
                ", date='" + date + '\'' +
                ", component=" + component +
                ", size=" + size +
                ", participant='" + participant + '\'' +
                '}';
    }
}
