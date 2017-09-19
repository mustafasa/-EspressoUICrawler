package com.example.arifm2.expressouicrawler.crawler;

import com.google.gson.annotations.Expose;

/**
 * Created by arifm2 on 9/15/2017.
 */

public class Content {
    @Expose
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


}
