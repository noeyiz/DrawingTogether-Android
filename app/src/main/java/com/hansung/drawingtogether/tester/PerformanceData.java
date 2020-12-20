package com.hansung.drawingtogether.tester;

public class PerformanceData {

    private String segment; // ss, ds, es
    private int size; // msg size

    private long start;
    private double time;

    public PerformanceData(String segment, long start) {
        this.segment = segment;
        this.start = start;
    }

    // 메시지 수신 시간 측정
    public void record(long end, int size) {
        this.time = (end - this.start)/1000.0; // 경과 시간 측정
        this.size = size;
    }

    // 화면 출력 시간 측정
    public void record(long end) {
        this.time = (end - this.start)/1000.0; // 경과 시간 측정
    }

    @Override
    public String toString() { // write to excel file
        return segment + "," + time + "," + size + "\n";
    }

}
