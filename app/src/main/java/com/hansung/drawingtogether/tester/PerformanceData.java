package com.hansung.drawingtogether.tester;

public class PerformanceData {

    private String segment; // ss, ds, es

    private long start;
    private double time;

    public PerformanceData(String segment, long start) {
        this.segment = segment;
        this.start = start;
    }

    // 화면 출력 시간 측정
    public void record(long end) {
        this.time = (end - this.start)/1000.0; // 경과 시간 측정
    }

    @Override
    public String toString() { // write to excel file
        return segment + "," + time + "\n";
    }

}
