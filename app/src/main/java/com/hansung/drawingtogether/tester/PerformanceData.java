package com.hansung.drawingtogether.tester;

import lombok.Getter;

@Getter
public class PerformanceData {

    private String sTag;
    private String eTag;

    private int size; // msg size

    private long start;
    private double time;

    public PerformanceData(long start) {
        this.start = start;
    }

    public PerformanceData(String sTag, long start) {
        this.sTag = sTag;
        this.start = start;
    }

    // join message zie + join ack message size
    public PerformanceData(long start, int size) {
        this.start = start;
        this.size = size;
    }

    // 메시지 수신 시간 측정
    public void record(long end, int size) {
        this.time = (end - this.start)/1000.0; // 경과 시간 측정
        this.size = this.size + size;
    }

    // 화면 출력 시간 측정
    public void record(long end) {
        this.time = (end - this.start)/1000.0; // 경과 시간 측정
    }

    public void record(String eTag, long end) {
        this.eTag = eTag;
        this.time = (end - this.start)/1000.0; // 경과 시간 측정
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String toStringPropagation() {
        return time + "," + size + "\n";
    }

    public String toStringDrawing() {
        return time + "\n";
    }

}