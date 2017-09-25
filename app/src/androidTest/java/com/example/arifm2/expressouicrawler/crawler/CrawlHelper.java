package com.example.arifm2.expressouicrawler.crawler;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.LayoutParams;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND;

/**
 * Utility class to take screenshots of activity screen
 */
public final class CrawlHelper {

  private static final String TAG = "CrawlHelper";

  /**
   * Takes screenshot of provided activity and puts it into bitmap.
   *
   * @param activity Activity of which the screenshot will be taken.
   * @return Bitmap of what is displayed in activity.
   * @throws UnableToTakeScreenshotException When there is unexpected error during taking
   *                                         screenshot
   */
  public static Bitmap getBitmap(Activity activity) {
    final List<ViewRootData> viewRoots = getRootViews(activity);
    if (viewRoots.isEmpty()) {
      throw new UnableToTakeScreenshotException("Unable to capture any view data in " + activity);
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

    // We need to do it in main thread
    if (Looper.myLooper() == Looper.getMainLooper()) {
      drawRootsToBitmap(viewRoots, bitmap);
    } else {
      final CountDownLatch latch = new CountDownLatch(1);
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          try {
            drawRootsToBitmap(viewRoots, bitmap);
          } finally {
            latch.countDown();
          }
        }
      });

      try {
        latch.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

      return bitmap;

  }

  private static void drawRootsToBitmap(List<ViewRootData> viewRoots, Bitmap bitmap) {
    for (ViewRootData rootData : viewRoots) {
      drawRootToBitmap(rootData, bitmap);
    }
  }

  private static void drawRootToBitmap(ViewRootData config, Bitmap bitmap) {
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


  @SuppressWarnings("unchecked") // no way to check
  public static List<ViewRootData> getRootViews(Activity activity) {
    Object globalWindowManager;
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
      globalWindowManager = getFieldValue("mWindowManager", activity.getWindowManager());
    } else {
      globalWindowManager = getFieldValue("mGlobal", activity.getWindowManager());
    }
    Object rootObjects = getFieldValue("mRoots", globalWindowManager);
    Object paramsObject = getFieldValue("mParams", globalWindowManager);

    Object[] roots;
    LayoutParams[] params;

    //  There was a change to ArrayList implementation in 4.4
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      roots = ((List) rootObjects).toArray();

      List<LayoutParams> paramsList = (List<LayoutParams>) paramsObject;
      params = paramsList.toArray(new LayoutParams[paramsList.size()]);
    } else {
      roots = (Object[]) rootObjects;
      params = (LayoutParams[]) paramsObject;
    }

    List<ViewRootData> rootViews = viewRootData(roots, params);
    if (rootViews.isEmpty()) {
      return Collections.emptyList();
    }
    return rootViews;
  }

  private static List<ViewRootData> viewRootData(Object[] roots, LayoutParams[] params) {
    List<ViewRootData> rootViews = new ArrayList<>();
    for (int i = 0; i < roots.length; i++) {
      Object root = roots[i];

      View view = (View) getFieldValue("mView", root);
      if (view == null) {
        Log.e(TAG, "null View stored as root in Global window manager, skipping");
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
    } catch (Exception e) {
      throw new UnableToTakeScreenshotException(e);
    }
    return target;
  }

  /**
   * Custom exception thrown if there is some exception thrown during
   * screenshot capturing to enable better client code exception handling.
   */
  public static class UnableToTakeScreenshotException extends RuntimeException {
    private UnableToTakeScreenshotException(String detailMessage) {
      super(detailMessage);
    }

    private UnableToTakeScreenshotException(String detailMessage, Exception exception) {
      super(detailMessage, extractException(exception));
    }

    private UnableToTakeScreenshotException(Exception ex) {
      super(extractException(ex));
    }

    /**
     * Method to avoid multiple wrapping. If there is already our exception,
     * just wrap the cause again
     */
    private static Throwable extractException(Exception ex) {
      if (ex instanceof UnableToTakeScreenshotException) {
        return ex.getCause();
      }

      return ex;
    }
  }

  public static class ViewRootData {


    private final View view;
    private final Rect winFrame;
    private final LayoutParams layoutParams;

    ViewRootData(View view, Rect winFrame, LayoutParams layoutParams) {
      this.view = view;
      this.winFrame = winFrame;
      this.layoutParams = layoutParams;
    }

    public View getView() {
      return view;
    }

    boolean isDialogType() {
      return layoutParams.type == LayoutParams.TYPE_APPLICATION;
    }

    boolean isActivityType() {
      return layoutParams.type == LayoutParams.TYPE_BASE_APPLICATION;
    }
  }


}