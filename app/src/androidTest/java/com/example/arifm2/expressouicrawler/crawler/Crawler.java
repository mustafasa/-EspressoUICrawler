package com.example.arifm2.expressouicrawler.crawler;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.arifm2.expressouicrawler.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND;

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

    //region Public API

    /**
     * Constructor passing String Resources and Local to be setup as current language.
     * This is should be initiated in espresso @BeforeClass.
     *
     * @param stringClass The {@link Class} of the String.
     * @param locale      Pass local language to be setup.
     */
    public Crawler(Class stringClass, Locale locale) {
        setLocale(locale);
        STRING_ID_COLLECTION = new HashMap<>();
        gson = new GsonBuilder().setPrettyPrinting().create();
        ENTRIES = new HashMap<>();
        this.STRING_CLASS = stringClass;

    }

    /**
     * The method is to setup initial information of specific (@link Activity) and reset SCREEN_ID
     * AND SCREEN_SEQUENCE_NUMBER to null.
     *
     * @param activity          The {@link Class} of the Activity.
     * @param hasMultipleScreen Indication to whether current test case has multiple screen to
     *                          captured. {@code true} to capture multi screen, {@code false} to
     *                          capture single screen.
     */
    public void setActivity(Activity activity, boolean hasMultipleScreen) {
        this.activity = activity;
        this.MULTIPLE_SCREEN = hasMultipleScreen;
        SCREEN_ID = null;
        SCREEN_SEQUENCE_NUMBER = null;
    }

    /**
     * Capture current screen image and capture current screen metadata.
     */
    public void capture() {
        SCREEN_ID = createScreenId();
        if (localization == null) {
            createLocalization();
        }
        createEntries();
        takeScreenshot();
    }

    /**
     * This method wraps up meta-data and write JSON file
     * This is should be called in espresso @AfterClass.
     */
    public void captureCompleted() {
        localization.setEntries(ENTRIES);
        createLocalizationFile();
    }
    //endregion

    //region Localization
    private void createLocalization() {
        localization = new Localization();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);
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
        for (ViewRootData rootViews : getRootViews()) {
            if (rootViews.isDialogType()) {
                setCoordinates(rootViews.getView().findViewById(android.R.id.button1), screenIdentifiers);
                setCoordinates(rootViews.getView().findViewById(android.R.id.button2), screenIdentifiers);
                setCoordinates(rootViews.getView().findViewById(android.R.id.message), screenIdentifiers);
                setCoordinates(rootViews.getView().findViewById(R.id.alertTitle), screenIdentifiers);
                return screenIdentifiers;
            }
        }
        ;
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
            } else if (view instanceof TextView) {
                setCoordinates(view, screenIdentifiers);
            } else {

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
        int[] locations = new int[2];
        view.getLocationOnScreen(locations);
        coordinates.setX(locations[0]);
        coordinates.setY(locations[1]);
        //Checking if coordinates already exist, if add in existing coordinate else add as new.
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
            OutputStream out = new FileOutputStream(jsonfile);
            byte[] buffer = gson.toJson(localization).getBytes();
            out.write(buffer);
            out.close();

        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    //endregion

    //region Screenshot
    private void takeScreenshot() {
        String iPath = createFolder().concat("/").concat(createImageFileName());
        View scrView = activity.getWindow().getDecorView().getRootView();
        scrView.setDrawingCacheEnabled(true);
//    Bitmap bitmap = Bitmap.createBitmap(scrView.getDrawingCache());
        Bitmap bitmap = getBitmap();
        scrView.setDrawingCacheEnabled(false);

        try {
            File imageFile = new File(iPath);
            OutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap getBitmap() {
        final List<ViewRootData> viewRoots = getRootViews();
        if (viewRoots.isEmpty()) {
            return null;
        }

        int maxWidth = Integer.MIN_VALUE;
        int maxHeight = Integer.MIN_VALUE;

        for (ViewRootData viewRoot : viewRoots) {
            if (viewRoot.winFrame.right > maxWidth) {
                maxWidth = viewRoot.winFrame.right;
            }

            if (viewRoot.winFrame.bottom > maxHeight) {
                maxHeight = viewRoot.winFrame.bottom;
            }
        }
        final Bitmap bitmap = Bitmap.createBitmap(maxWidth, maxHeight, ARGB_8888);

        for (ViewRootData rootData : viewRoots) {
            drawRootToBitmap(rootData, bitmap);
        }
        return bitmap;

    }

    private List<ViewRootData> getRootViews() {
        Object globalWindowManager;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            globalWindowManager = getFieldValue("mWindowManager", activity.getWindowManager());
        } else {
            globalWindowManager = getFieldValue("mGlobal", activity.getWindowManager());
        }
        Object rootObjects = getFieldValue("mRoots", globalWindowManager);
        Object paramsObject = getFieldValue("mParams", globalWindowManager);

        Object[] roots;
        WindowManager.LayoutParams[] params;

        //  There was a change to ArrayList implementation in 4.4
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            roots = ((List) rootObjects).toArray();

            List<WindowManager.LayoutParams> paramsList = (List<WindowManager.LayoutParams>) paramsObject;
            params = paramsList.toArray(new WindowManager.LayoutParams[paramsList.size()]);
        } else {
            roots = (Object[]) rootObjects;
            params = (WindowManager.LayoutParams[]) paramsObject;
        }

        List<ViewRootData> rootViews = viewRootData(roots, params);
        if (rootViews.isEmpty()) {
            return Collections.emptyList();
        }
        return rootViews;
    }

    private void drawRootToBitmap(ViewRootData config, Bitmap bitmap) {
        // now only dim supported
        if ((config.layoutParams.flags & FLAG_DIM_BEHIND) == FLAG_DIM_BEHIND) {
            Canvas dimCanvas = new Canvas(bitmap);

            int alpha = (int) (255 * config.layoutParams.dimAmount);
            dimCanvas.drawARGB(alpha, 0, 0, 0);
        }

        Canvas canvas = new Canvas(bitmap);
        canvas.translate(config.winFrame.left, config.winFrame.top);
        config.view.draw(canvas);
    }

    private List<ViewRootData> viewRootData(Object[] roots, WindowManager.LayoutParams[] params) {
        List<ViewRootData> rootViews = new ArrayList<>();
        for (int i = 0; i < roots.length; i++) {
            Object root = roots[i];

            View view = (View) getFieldValue("mView", root);
            if (view == null) {
                continue;
            }

            if (!view.isShown()) {
                continue;
            }

            Object attachInfo = getFieldValue("mAttachInfo", root);
            int top = (int) getFieldValue("mWindowTop", attachInfo);
            int left = (int) getFieldValue("mWindowLeft", attachInfo);

            Rect winFrame = (Rect) getFieldValue("mWinFrame", root);
            Rect area = new Rect(left, top, left + winFrame.width(), top + winFrame.height());

            rootViews.add(new ViewRootData(view, area, params[i]));
        }

        return rootViews;
    }

    private Object getFieldValue(String fieldName, Object target) {
        try {
            Class currentClass = target.getClass();
            while (currentClass != Object.class) {
                for (Field currentField : currentClass.getDeclaredFields()) {
                    if (fieldName.equals(currentField.getName())) {
                        Field field = currentField;
                        field.setAccessible(true);
                        return field.get(target);
                    }
                }
                currentClass = currentClass.getSuperclass();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return target;
    }

    private class ViewRootData {

        private final View view;
        private final Rect winFrame;
        private final WindowManager.LayoutParams layoutParams;

        ViewRootData(View view, Rect winFrame, WindowManager.LayoutParams layoutParams) {
            this.view = view;
            this.winFrame = winFrame;
            this.layoutParams = layoutParams;
        }

        View getView() {
            return view;
        }

        boolean isDialogType() {
            return layoutParams.type == WindowManager.LayoutParams.TYPE_APPLICATION;
        }

        boolean isActivityType() {
            return layoutParams.type == WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
        }
    }
    //endregion

    //region Utilities
    private void setLocale(Locale locale) {
        Resources resources = InstrumentationRegistry.getTargetContext().getResources();
        Locale.setDefault(locale);
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

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
                //TODO: add actual project name
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
        return Integer.toString(activity.getClass().getSimpleName().hashCode(), 36);
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
        //Escape special cases
        result = result.replaceAll("\\!", "\\\\!");
        result = result.replaceAll("%%", "%");

        if (result.indexOf("%") >= 0) {
            result = result.replaceAll("%s", "[\\\\w]+").replaceAll("%d", "[\\\\w]+");
            //This lopp is more specific to examine length of number
            // eg:start%3 ==start001
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
    //endregion

}