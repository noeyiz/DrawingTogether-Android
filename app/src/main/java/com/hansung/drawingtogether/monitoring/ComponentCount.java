package com.hansung.drawingtogether.monitoring;


public class ComponentCount {

    private String topic;
    private int stroke;
    private int rect;
    private int oval;
    private int text;
    private int image;
    private int erase;

    public ComponentCount(String topic) {
        this.topic = topic;
        this.stroke = 0;
        this.rect = 0;
        this.oval = 0;
        this.text = 0;
        this.image = 0;
        this.erase = 0;
    }

    public void increaseStroke() { stroke++; }

    public void increaseRect() { rect++; }

    public void increaseOval() { oval++; }

    public void increaseText() { text++; }

    public void increaseImage() { image++; }

    public void increaseErase() { erase++; }

//    public void decreaseStroke() { stroke--; }
//
//    public void decreaseRect() { rect--; }
//
//    public void decreaseOval() { oval--; }
//
//    public void decreaseText() { text--; }
//
//    public void clearCount() {
//        this.stroke = 0;
//        this.rect = 0;
//        this.oval = 0;
//        this.text = 0;
//    }

}
