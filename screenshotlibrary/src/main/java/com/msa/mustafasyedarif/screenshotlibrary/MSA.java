package com.msa.mustafasyedarif.screenshotlibrary;

import android.app.Activity;

import java.util.ArrayList;

import static com.msa.mustafasyedarif.screenshotlibrary.CaptureScreenIdentifier.getScreenIdentifiers;
import static com.msa.mustafasyedarif.screenshotlibrary.CaptureScreenShot.takeScreenshot;

/**
 * Created by Mustafa Arif on 10/10/2017.
 */

public final class MSA {

    private static String folderName = "MSA";

    /**
     * This method capture screenshot current activity view and save file by user specified name.
     *
     * @param activity       The {@link Class} of the Activity.
     * @param screenShotName Provide a name to make unique name for screen shot.
     * @return <code>true</code> if the screen capture was successful, <code>false</code>
     * otherwise.
     */
    public static boolean CaptureScreenShot(Activity activity, String screenShotName) {
        return takeScreenshot(activity, screenShotName, folderName);
    }

    /**
     * This method capture screenshot of current activity view in auto-generate sequence Number,
     * so user doesn't need to pass name for screenshot.
     *
     * @param activity The {@link Class} of the Activity.
     * @return <code>true</code> if the screen capture was successful, <code>false</code>
     * otherwise.
     */
    public static boolean CaptureScreenShot(Activity activity) {
        return takeScreenshot(activity, null, folderName);
    }

    /**
     * This method capture screen ui elements meta data of current activity.
     *
     * @param activity The {@link Class} of the Activity.
     * @return {@link <ArrayList<ScreenIdentifier>} of all identifier on current activity.
     */
    public static ArrayList<ScreenIdentifier> CaptureScreenIdentifier(Activity activity) {
        return getScreenIdentifiers(activity);
    }

    /**
     * This method is used to set user define folder name, it is optional if not provided it will
     * utilized default folder name.
     *
     * @param name Provide a name for folder.
     **/
    public static void setFolderName(String name) {
        folderName = name;
    }

}
