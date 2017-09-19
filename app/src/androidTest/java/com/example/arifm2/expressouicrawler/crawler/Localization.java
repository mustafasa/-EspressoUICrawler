package com.example.arifm2.expressouicrawler.crawler;

import com.google.gson.annotations.Expose;

import java.util.Map;

/**
 * Created by arifm2 on 9/13/2017.
 */

public class Localization {
    @Expose
    private String timeStamp;
    @Expose
    private String version;
    @Expose
    private String build;
    @Expose
    private String device;
    @Expose
    private String locale;
    @Expose
    Map<String, Content> identifiers;

    public Map<String, Content> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Map<String, Content> identifiers) {
        this.identifiers = identifiers;
    }

    @Expose
    Map<String, Entries> entries;

    public Map<String, Entries> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, Entries> entries) {
        this.entries = entries;
    }



    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

}
