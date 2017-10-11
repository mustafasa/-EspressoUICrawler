package com.msa.mustafasyedarif.screenshotlibrary;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.os.Looper;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND;
import static com.msa.mustafasyedarif.screenshotlibrary.CaptureRootView.getRootViews;

/**
 * Created by arifm2 on 10/10/2017.
 */

/**
 * {@link CaptureScreenShot}eenshot class is utilized to create screen shot of current visible you element, it is
 * capable of capturing all UI child element present on the current {@link Activity},child element
 * such as Dialog-box,Pop-up box, Fragment and many more.
 * This class is utilized by {@link MSA}
 */
public final class CaptureScreenShot {

    /**
     * This method capture screenshot current activity view and save file by user specified name.
     *
     * @param activity   The {@link Class} of the Activity.
     * @param fileName   Provide a file name to be saved.
     * @param folderName Provide a folder name to be saved.
     * @return <code>true</code> if the screen capture was successful, <code>false</code>
     * otherwise.
     */
    protected static boolean takeScreenshot(Activity activity, String fileName, String folderName) {

        String iPath = createFolder(folderName)
                .concat("/")
                .concat(createImageFileName(fileName));

        View scrView = activity.getWindow().getDecorView().getRootView();
        scrView.setDrawingCacheEnabled(true);
        Bitmap bitmap = getBitmap(activity);
        scrView.setDrawingCacheEnabled(false);
        try {
            File imageFile = new File(iPath);
            OutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static Bitmap getBitmap(Activity activity) {
        final List<CaptureRootView.ViewRootData> viewRoots = getRootViews(activity);
        if (viewRoots.isEmpty()) {
            return null;
        }

        int maxWidth = Integer.MIN_VALUE;
        int maxHeight = Integer.MIN_VALUE;

        for (CaptureRootView.ViewRootData viewRoot : viewRoots) {
            if (viewRoot.winFrame.right > maxWidth) {
                maxWidth = viewRoot.winFrame.right;
            }

            if (viewRoot.winFrame.bottom > maxHeight) {
                maxHeight = viewRoot.winFrame.bottom;
            }
        }
        final Bitmap bitmap = Bitmap.createBitmap(maxWidth, maxHeight, ARGB_8888);
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

    private static void drawRootsToBitmap(List<CaptureRootView.ViewRootData> viewRoots, Bitmap bitmap) {
        for (CaptureRootView.ViewRootData rootData : viewRoots) {
            if ((rootData.layoutParams.flags & FLAG_DIM_BEHIND) == FLAG_DIM_BEHIND) {
                Canvas dimCanvas = new Canvas(bitmap);
                int alpha = (int) (255 * rootData.layoutParams.dimAmount);
                dimCanvas.drawARGB(alpha, 0, 0, 0);
            }
            Canvas canvas = new Canvas(bitmap);
            canvas.translate(rootData.winFrame.left, rootData.winFrame.top);
            rootData.view.draw(canvas);
        }
    }

    private static String createImageFileName(String fileName) {
        String date = DateFormat.getDateInstance().format(new Date());
        if (fileName == null) {
            return date.concat(".png");
        }
        return fileName.concat(".png");
    }

    private static String createFolder(String name) {
        String folderName = (Environment.getExternalStorageDirectory().getAbsolutePath().toString())
                .concat("/")
                .concat(name)
                .concat("/");

        File dir = new File(folderName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return folderName;
    }
}
