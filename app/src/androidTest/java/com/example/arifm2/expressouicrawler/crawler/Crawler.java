package com.example.arifm2.expressouicrawler.crawler;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.arifm2.expressouicrawler.R;
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
  private Gson gson;
  private Localization localization;
  private String SCREEN_ID;
  private Map<String, String> STRING_ID_COLLECTION;
  private Map<String, Entries> ENTRIES;
  private boolean MULTIPLE_SCREEN;
  private Integer SCREEN_SEQUENCE_NUMBER;
  private Class STRING_CLASS;


  public Crawler(Class stringClass) {
    STRING_ID_COLLECTION = new HashMap<>();
    gson = new Gson();
    ENTRIES = new HashMap<>();
    this.STRING_CLASS = stringClass;

  }

  public void setActivity(Activity activity, boolean multipleScreen) {
    this.activity = activity;
    this.MULTIPLE_SCREEN = multipleScreen;
    SCREEN_ID = null;
    SCREEN_SEQUENCE_NUMBER = null;
  }

  public void setLocale(Locale locale) {
    Resources resources = InstrumentationRegistry.getTargetContext().getResources();
    Locale.setDefault(locale);
    Configuration config = resources.getConfiguration();
    config.locale = locale;
    resources.updateConfiguration(config, resources.getDisplayMetrics());
  }

  public void capture() {
    SCREEN_ID = createScreenId();
    if (localization == null) {
      createLocalization();
    }
    takeScreenshot();
    createEntries();
  }

  public void captureCompleted() {
    localization.setEntries(ENTRIES);
    createLocalizationFile();
  }

  private void createLocalization() {
    localization = new Localization();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss", Locale.ENGLISH);
    try {
      PackageInfo info = activity.getPackageManager()
          .getPackageInfo(activity.getPackageName(), 0);
      localization.setTimeStamp(sdf.format(new Date()));
      localization.setVersion(info.versionName);
      //TODO: add actual build number
      localization.setBuild(info.versionName);
      localization.setDevice(android.os.Build.MODEL);
      localization.setLocale(activity.getResources().getConfiguration().locale.toString());
      localization.setIdentifiers(getAllIdentifiers());
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
  }

  private Map<String, Content> getAllIdentifiers() {
    Map<String, Content> identifier = new HashMap<>();
    for (final Field field : STRING_CLASS.getFields()) {
      String idInString = field.getName();
      try {
        int stringId = field.getInt(STRING_CLASS);
        String stringValue = activity.getResources().getString(stringId);
        Content content = new Content();
        content.setContent(stringValue);
        identifier.put(idInString, content);
        STRING_ID_COLLECTION.put(stringValue, idInString);
      } catch (Exception ex) {
        //do smth
      }
    }
    return identifier;
  }

  private void createEntries() {
    ENTRIES.put(SCREEN_ID, createSubEntries());
  }

  private Entries createSubEntries() {
    Entries subEntries = new Entries();
    subEntries.setName(createImageName());
    subEntries.setFileName(createImageFileName());
    subEntries.setIdentifiers(getCurrentScreenIdentifiers());
    return subEntries;
  }

  private Map<String, List<Coordinates>> getCurrentScreenIdentifiers() {
    Map<String, List<Coordinates>> screenIdentifiers = new HashMap<>();
    //This is dialog box reader, creating issue while using, it is creating empty json file.
//    for(CrawlHelper.ViewRootData rootViews: CrawlHelper.getRootViews(activity)){
//      if(rootViews.isDialogType()){
//        setCoordinates(rootViews.getView().findViewById(android.R.id.button1),screenIdentifiers);
//        setCoordinates(rootViews.getView().findViewById(android.R.id.button2),screenIdentifiers);
//        setCoordinates(rootViews.getView().findViewById(android.R.id.message),screenIdentifiers);
//        setCoordinates(rootViews.getView().findViewById(R.id.alertTitle),screenIdentifiers);
//        return screenIdentifiers;
//      }
//    };
    ViewGroup viewgroup = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content))
        .getChildAt(0);
    getChildIdentifier(viewgroup, screenIdentifiers);
    return screenIdentifiers;
  }

  private void getChildIdentifier(ViewGroup viewgroup,
      Map<String, List<Coordinates>> screenIdentifiers) {
    for (int i = 0; i < viewgroup.getChildCount(); i++) {
      View view = viewgroup.getChildAt(i);
      if (view instanceof ViewGroup) {
        getChildIdentifier((ViewGroup) view, screenIdentifiers);
      } else {
        setCoordinates(view, screenIdentifiers);
      }
    }
  }

  private void setCoordinates(View view, Map<String, List<Coordinates>> screenIdentifiers) {
    if (view == null) {
      return;
    }
    String stringId = convertStringToStringId(((TextView) view).getText().toString());
    if (stringId == null) {
      return;
    }
    Coordinates coordinates = new Coordinates();
    coordinates.setWidth(view.getWidth());
    coordinates.setHeight(view.getHeight());
    coordinates.setX((int) view.getX());
    coordinates.setY((int) view.getY());
    //Checking if coordinates already exist if then add in existing else add as new.
    if (screenIdentifiers.containsKey(stringId)) {
      List<Coordinates> list = screenIdentifiers.get(stringId);
      list.add(coordinates);
      screenIdentifiers.put(stringId, list);
    } else {
      List<Coordinates> list = new ArrayList<>();
      list.add(coordinates);
      screenIdentifiers.put(stringId, list);
    }
  }

  private String convertStringToStringId(String s) {
    if (!STRING_ID_COLLECTION.containsKey(s)) {
      for (String key : STRING_ID_COLLECTION.keySet()) {
        if (s.matches(getRegexpFromFormatString(key))) {
          return STRING_ID_COLLECTION.get(key);
        }
      }
    }
    return STRING_ID_COLLECTION.get(s);
  }

  private void createLocalizationFile() {
    String tPath = (createFolder() + "/localization.json");
    try {
      File jsonfile = new File(tPath);
      jsonfile.createNewFile();
      makeSureFileIsVisibleViaUsb(jsonfile);
      OutputStream fo = new FileOutputStream(jsonfile);
      byte[] buffer = gson.toJson(localization).getBytes();
      fo.write(buffer);
      fo.close();

    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private void takeScreenshot() {
    String iPath = createFolder().concat("/").concat(createImageFileName());
    View scrView = activity.getWindow().getDecorView().getRootView();
    scrView.setDrawingCacheEnabled(true);
    Bitmap bitmap = CrawlHelper.getBitmap(activity);
    scrView.setDrawingCacheEnabled(false);

    try {
      File imageFile = new File(iPath);
      OutputStream out = new FileOutputStream(imageFile);
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
      out.flush();
      out.close();
    } catch (FileNotFoundException e) {
      // exception
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //Utilities
  private String createImageName() {
    if (!MULTIPLE_SCREEN) {
      return splitCamelCase(activity.getClass().getSimpleName().replace("Activity", ""));
    }

    return splitCamelCase(activity.getClass().getSimpleName().replace("Activity", ""))
        .concat(" [")
        .concat(String.valueOf(SCREEN_SEQUENCE_NUMBER))
        .concat("]");
  }

  private String createImageFileName() {
    if (!MULTIPLE_SCREEN) {
      return splitCamelCase(activity.getClass().getSimpleName()
          .replace("Activity", ""))
          .concat(".png");
    }

    return splitCamelCase(activity.getClass().getSimpleName().replace("Activity", ""))
        .concat("-")
        .concat(String.valueOf(SCREEN_SEQUENCE_NUMBER))
        .concat(".png");
  }

  private String createFolder() {
    String folderName = (Environment.getExternalStorageDirectory().getAbsolutePath().toString())
        .concat("/")
        .concat("Mustafa")
        .concat("/")
        .concat(activity.getResources().getConfiguration().locale.toString());

    File dir = new File(folderName);
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return folderName;
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
    if (!MULTIPLE_SCREEN) {
      return createId();
    }

    if (SCREEN_ID == null) {
      SCREEN_ID = createId();
    }
    return SCREEN_ID.split("-")[0]
        .concat("-")
        .concat(String.format(Locale.US, "%02d", getNumberForScreen()));

  }

  private int getNumberForScreen() {
    if (SCREEN_SEQUENCE_NUMBER == null) {
      SCREEN_SEQUENCE_NUMBER = 0;
    } else {
      SCREEN_SEQUENCE_NUMBER = SCREEN_SEQUENCE_NUMBER + 1;
    }

    return SCREEN_SEQUENCE_NUMBER;
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
    result = result.replaceAll("%%", "%");

    if (result.indexOf("%") >= 0) {
      result = result.replaceAll("%s", "[\\\\w]+").replaceAll("%d", "[\\\\w]+");

      while (result.matches(".*%([0-9]+)[d]{1}.*")) {
        String digitStr = result.replaceFirst(".*%([0-9]+)[d]{1}.*", "$1");
        int numDigits = Integer.parseInt(digitStr);
        result = result.replaceFirst("(.*)(%[0-9]+[d]{1})(.*)", "$1[0-9]{"
            + numDigits + "}$3");
      }
    }

    return "^"
        .concat(result)
        .concat("$");
  }

}