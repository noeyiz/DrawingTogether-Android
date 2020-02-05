package com.hansung.drawingtogether.view.history;

public class HistoryData {

    private int image; // 이미지뷰에 넣을 데이터 ! 우선 ex_.png 사용할거라 int로
    private String topic;
    private String time;

    public HistoryData(int image, String topic, String time) {
        this.image = image;
        this.topic = topic;
        this.time = time;
    }

    public int getImage() {
        return image;
    }

    public String getTopic() {
        return topic;
    }

    public String getTime() {
        return time;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
