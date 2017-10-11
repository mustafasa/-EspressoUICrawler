package com.msa.mustafasyedarif.screenshotlibrary;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by arifm2 on 10/10/2017.
 */

/**
 * Utility class to capture root View
 */
public class CaptureRootView {

    /**
     * This method get current activity root view windowManager.
     *
     * @param activity   The {@link Class} of the Activity.
     */
    protected static List<ViewRootData> getRootViews(Activity activity) {
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


    private static List<ViewRootData> viewRootData(Object[] roots,
                                                     WindowManager.LayoutParams[] params) {
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

    private static Object getFieldValue(String fieldName, Object target) {
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
    /**
     * {@link ViewRootData} class is holder class for datatype.
     */
    protected static class ViewRootData {

        protected final View view;
        protected final Rect winFrame;
        protected final WindowManager.LayoutParams layoutParams;

        ViewRootData(View view, Rect winFrame, WindowManager.LayoutParams layoutParams) {
            this.view = view;
            this.winFrame = winFrame;
            this.layoutParams = layoutParams;
        }

        View getView() {
            return view;
        }

        boolean isDialog() {
            return layoutParams.type == WindowManager.LayoutParams.TYPE_APPLICATION;
        }

        boolean isPopupWindow() {
            return layoutParams.type == WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        }

        boolean isActivity() {
            return layoutParams.type == WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
        }
    }
}

