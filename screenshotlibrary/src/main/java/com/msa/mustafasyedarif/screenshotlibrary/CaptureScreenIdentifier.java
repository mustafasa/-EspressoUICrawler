package com.msa.mustafasyedarif.screenshotlibrary;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import static com.msa.mustafasyedarif.screenshotlibrary.CaptureRootView.getRootViews;

/**
 * Created by arifm2 on 10/10/2017.
 */

/**
 * {@link CaptureScreenIdentifier} class is utilized to get current activity UI element meta data
 * information.
 * This class is utilized by {@link MSA}
 */
public final class CaptureScreenIdentifier {

    /**
     * This method capture screen UI elements meta data of current activity.
     *
     * @param activity The {@link Class} of the Activity.
     * @return {@link <ArrayList<ScreenIdentifier>} of all identifier on current activity.
     * otherwise.
     */
    protected static ArrayList<ScreenIdentifier> getScreenIdentifiers(Activity activity) {
        ArrayList<ScreenIdentifier> currentScreenIdentifiers = new ArrayList<>();
        for (CaptureRootView.ViewRootData rootViews : getRootViews(activity)) {
            if (rootViews.isDialog()) {
                ViewGroup viewgroup = (ViewGroup) rootViews.getView();
                setScreenIdentifierFromViewgroup(viewgroup, currentScreenIdentifiers);
                return currentScreenIdentifiers;
            }
        }
        for (CaptureRootView.ViewRootData rootViews : getRootViews(activity)) {
            ViewGroup viewgroup = (ViewGroup) rootViews.getView();
            setScreenIdentifierFromViewgroup(viewgroup, currentScreenIdentifiers);
        }
        return currentScreenIdentifiers;
    }

    private static void setScreenIdentifierFromViewgroup
            (ViewGroup viewgroup, ArrayList<ScreenIdentifier> currentScreenIdentifiers) {
        for (int i = 0; i < viewgroup.getChildCount(); i++) {
            View view = viewgroup.getChildAt(i);

            if (view instanceof ViewGroup) {
                setScreenIdentifierFromViewgroup((ViewGroup) view, currentScreenIdentifiers);
            } else if (view instanceof TextView) {
                setScreenIdentifierFromView(view, currentScreenIdentifiers);
            } else {
                //do something else
            }
        }
    }

    private static void setScreenIdentifierFromView(View view, ArrayList<ScreenIdentifier>
            currentScreenIdentifiers) {
        ScreenIdentifier screenIdentifiers = new ScreenIdentifier();
        screenIdentifiers.setUiWidth(view.getWidth());
        screenIdentifiers.setUiHeight(view.getHeight());
        int[] locations = new int[2];
        view.getLocationOnScreen(locations);
        screenIdentifiers.setUiXaxis(locations[0]);
        screenIdentifiers.setUiYaxis(locations[1]);
        screenIdentifiers.setUiText(((TextView) view).getText().toString());
        screenIdentifiers.setUiVisibility(view.getVisibility() == View.VISIBLE);
        screenIdentifiers.setUiType(view.getClass().getName());
        if (view.getId() != -1) {
            screenIdentifiers.setUiIdentifier(view.getResources().getResourceEntryName(view.getId()));
        }

        currentScreenIdentifiers.add(screenIdentifiers);
    }

}
