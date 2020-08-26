package com.hansung.drawingtogether.data.remote.model;


public class ComponentCount {
    int stroke;
    int rect;
    int oval;
    int text;

    public ComponentCount() {
        this.stroke = 0;
        this.rect = 0;
        this.oval = 0;
        this.text = 0;
    }

    public void increaseStroke() { stroke++; }

    public void increaseRect() { rect++; }

    public void increaseOval() { oval++; }

    public void increaseText() { text++; }

    public void decreaseStroke() { stroke--; }

    public void decreaseRect() { rect--; }

    public void decreaseOval() { oval--; }

    public void decreaseText() { text--; }
}
