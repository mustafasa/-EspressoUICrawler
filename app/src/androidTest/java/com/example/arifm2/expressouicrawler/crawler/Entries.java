package com.example.arifm2.expressouicrawler.crawler;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

/**
 * Created by arifm2 on 9/14/2017.
 */

public class Entries {
    @Expose
    private String name;
    @Expose
    private String fileName;
    @Expose
    Map<String, List<CoOrdinates>> identifiers;

    public Map<String, List<CoOrdinates>> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Map<String, List<CoOrdinates>> identifiers) {
        this.identifiers = identifiers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }




}
