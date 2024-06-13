package com.stardust.autojs.core.opencv;

import android.content.Context;

import android.os.Looper;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;


/**
 * Created by Stardust on 2018/4/2.
 */
public class OpenCVHelper {

    public interface InitializeCallback {
        void onInitFinish();
    }

    private static final String LOG_TAG = "OpenCVHelper";
    private static boolean sInitialized = false;

    public static MatOfPoint newMatOfPoint(Mat mat) {
        return new MatOfPoint(mat);
    }

    public static void release(MatOfPoint mat) {
        if (mat == null)
            return;
        mat.release();
    }

    public static void release(Mat mat) {
        if (mat == null)
            return;
        mat.release();
    }

    public synchronized static boolean isInitialized() {
        return sInitialized;
    }

    public synchronized static void initIfNeeded(Context context, InitializeCallback callback) {
        if (sInitialized) {
            callback.onInitFinish();
            return;
        }
        sInitialized = true;
        if (Looper.getMainLooper() == Looper.myLooper()) {
            new Thread(() -> {
                OpenCVLoader.initDebug();
                callback.onInitFinish();
            }).start();
        } else {
            OpenCVLoader.initDebug();
            callback.onInitFinish();
        }
    }
}
