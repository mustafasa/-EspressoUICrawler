package com.example.arifm2.expressouicrawler.crawler;

import com.google.gson.annotations.Expose;

/**
 * Created by arifm2 on 9/15/2017.
 */

public class Coordinates {
    @Expose
    private int x;
    @Expose
    private int y;
    @Expose
    private int width;
    @Expose
    private int height;
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }


}
