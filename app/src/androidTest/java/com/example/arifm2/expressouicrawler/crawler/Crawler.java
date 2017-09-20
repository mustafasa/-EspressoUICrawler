package com.example.arifm2.expressouicrawler.crawler;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Created by arifm2 on 9/15/2017.
 */

public class Crawler {
    private Activity activity;
    private int resource;
    private String screenID;
    private Map<String, String> Stringids;
    private Gson gson;
    private Localization localization;
    private boolean hasMultipleScreen;
    private Integer sequenceNumber;
    private Map<String, Entries> entries;
    private Object stringClass;
    private Field[] fields;

    public Crawler(Object stringClass, Field[] fields) {
        Stringids = new HashMap<>();
        gson = new Gson();
        entries = new HashMap<>();
        this.stringClass = stringClass;
        this.fields = fields;

    }

    public void setActivity(Activity activity, int resource, boolean hasMultipleScreen) {
        this.activity = activity;
        this.resource = resource;
        this.hasMultipleScreen = hasMultipleScreen;
        screenID = null;
        sequenceNumber = null;
    }

    public void setLocale(Locale locale) {
        Resources resources = InstrumentationRegistry.getTargetContext().getResources();
        Locale.setDefault(locale);
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    public void capture() {
        screenID = createScreenId();
        if (localization == null) {
            createLocalization();
        }
        createEntries();
        takeScreenshot();
    }

    public void captureCompleted() {
        localization.setEntries(entries);
        createLocalizationFile();
    }

    private void createLocalization() {
        localization = new Localization();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss", Locale.ENGLISH);
        localization.setTimeStamp(sdf.format(new Date()));
        localization.setVersion("1");
        localization.setBuild("1");
        localization.setDevice(android.os.Build.MODEL);
        localization.setLocale(activity.getResources().getConfiguration().locale.toString());
        localization.setIdentifiers(getAllIdentifiers());

    }

    private Map<String, Content> getAllIdentifiers() {
        Map<String, Content> identifier = new HashMap<>();
        for (final Field field : fields) {
            String idInString = field.getName();
            try {
                int stringId = field.getInt(stringClass);
                String stringValue = activity.getResources().getString(stringId);
                Content content = new Content();
                content.setContent(stringValue);
                identifier.put(idInString, content);
                Stringids.put(stringValue, idInString);
            } catch (Exception ex) {
                //do smth
            }
        }
        return identifier;
    }

    private void createEntries() {
        entries.put(screenID, createSubEntries());
    }

    private Entries createSubEntries() {
        Entries subEntries = new Entries();
        subEntries.setName(createImageName());
        subEntries.setFileName(createImageFileName());
        subEntries.setIdentifiers(getCurrentScreenIdentifiers());
        return subEntries;
    }

    private Map<String, List<CoOrdinates>> getCurrentScreenIdentifiers() {
        Map<String, List<CoOrdinates>> identifiers = new HashMap<>();
        LayoutInflater inflater = activity.getLayoutInflater();
        ViewGroup viewgroup = (ViewGroup) inflater.inflate(resource, null, true);
        for (int i = 0; i < viewgroup.getChildCount(); i++) {
            View view = viewgroup.getChildAt(i);
            CoOrdinates coordinates = new CoOrdinates();
            String id = "";
            if (view instanceof Button) {
                Button buttonView = activity.findViewById(view.getId());
                id = Stringids.get(buttonView.getText().toString());
                coordinates.setWidth(buttonView.getWidth());
                coordinates.setHeight(buttonView.getHeight());
                coordinates.setX((int) buttonView.getX());
                coordinates.setY((int) buttonView.getY());
            } else if (view instanceof TextView) {
                TextView textView = activity.findViewById(view.getId());
                id = getIdFromString(textView.getText().toString());
                coordinates.setWidth(textView.getWidth());
                coordinates.setHeight(textView.getHeight());
                coordinates.setX((int) textView.getX());
                coordinates.setY((int) textView.getY());
            }
            //Adding CoOrdinates in identifier, if CoOrdinates exist add new value
            // in existing CoOrdinates array
            if (identifiers.containsKey(id)) {
                List<CoOrdinates> list = identifiers.get(id);
                list.add(coordinates);
                identifiers.put(id, list);
            } else {
                List<CoOrdinates> list = new ArrayList<>();
                list.add(coordinates);
                identifiers.put(id, list);
            }

        }
        return identifiers;
    }

    private String getIdFromString(String s) {
        if (!Stringids.containsKey(s)) {
            for (String key : Stringids.keySet()) {
                if (s.matches(getRegexpFromFormatString(key)))
                    return Stringids.get(key);
            }
        }
        return Stringids.get(s);
    }

    private void createLocalizationFile() {
        String tPath = (getFolderName() + "/localization.json");
        File dir = new File(getFolderName());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            File jsonfile = new File(tPath);
            makeSureFileIsVisibleViaUsb(jsonfile);
            if (!jsonfile.exists()) {
                jsonfile.createNewFile();
                OutputStream fo = new FileOutputStream(jsonfile);
                byte[] buffer = gson.toJson(localization).getBytes();
                fo.write(buffer);
                fo.close();
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void takeScreenshot() {
        String iPath = (getFolderName() + "/" + createImageFileName());
        File dir = new File(getFolderName());

        if (!dir.exists()) {
            dir.mkdirs();
            makeSureFileIsVisibleViaUsb(dir);
        }

        View scrView = activity.getWindow().getDecorView().getRootView();
        scrView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(scrView.getDrawingCache());
        scrView.setDrawingCacheEnabled(false);

        OutputStream out;
        File imageFile = new File(iPath);

        try {
            out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            // exception
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFolderName() {
        return (Environment.getExternalStorageDirectory().getAbsolutePath().toString()
                + "/" + "Mustafa/" + createFolderName());
    }

    private String createImageName() {
        if (!hasMultipleScreen) {
            return splitCamelCase(activity.getClass().getSimpleName().replace("Activity", ""));
        }

        return splitCamelCase(activity.getClass().getSimpleName().replace("Activity", "")) +
                " [" + sequenceNumber + "]";
    }

    private String createImageFileName() {
        if (!hasMultipleScreen) {
            return splitCamelCase(activity.getClass().getSimpleName()
                    .replace("Activity", "")) + ".png";
        }

        return splitCamelCase(activity.getClass().getSimpleName().replace("Activity", "")) + "-" +
                sequenceNumber + ".png";
    }

    private String createFolderName() {
        return activity.getResources().getConfiguration().locale.toString();
    }

    private String createId() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 8) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }

    private String createScreenId() {
        if (!hasMultipleScreen) {
            return createId();
        }

        if (screenID == null) {
            screenID = createId();
        }
        return screenID.split("-")[0] + "-" + String.format(Locale.ENGLISH, "%02d",
                getNumberForScreen());

    }

    private int getNumberForScreen() {
        if (sequenceNumber == null) {
            sequenceNumber = 0;
        } else {
            sequenceNumber = sequenceNumber + 1;
        }

        return sequenceNumber;
    }

    private String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }

    private void makeSureFileIsVisibleViaUsb(File file) {
        try {
            if (file.exists()) {
                MediaScannerConnection.scanFile(activity,
                        new String[]{file.getAbsolutePath()}, null, null);
            }
        } catch (Throwable ignore) {
        }
    }

    private String getRegexpFromFormatString(String format) {
        String result = format;
        result = result.replaceAll("\\!", "\\\\!");

        if (result.indexOf("%") >= 0) {
            result = result.replaceAll("%s", "[\\\\w]+").replaceAll("%d", "[\\\\w]+");

            while (result.matches(".*%([0-9]+)[d]{1}.*")) {
                String digitStr = result.replaceFirst(".*%([0-9]+)[d]{1}.*", "$1");
                int numDigits = Integer.parseInt(digitStr);
                result = result.replaceFirst("(.*)(%[0-9]+[d]{1})(.*)", "$1[0-9]{"
                        + numDigits + "}$3");
            }
        }

        return "^" + result + "$";
    }

}