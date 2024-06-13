package com.tony.yolov8ncnn;

import android.util.Log;

import org.opencv.core.Mat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author TonyJiangWJ
 * @since 2024/6/1
 */
public class NcnnPredictorNative {
    public NcnnPredictorNative() {
        initLibrary();
    }

    private static final AtomicBoolean isSOLoaded = new AtomicBoolean(false);
    public native boolean loadModel(String paramPath, String binPath, int imageSize, int numClass, int cpugpu);

    public native List<PredictResult> predictYolo(long matAddress, float confThreshold, float nmsThreshold);

    public native List<PredictResult> predictYoloByPath(String imagePath, float confThreshold, float nmsThreshold);

    public native void release();
    public List<PredictResult> predictYolo(Mat mat, float confThreshold, float nmsThreshold) {
        return predictYolo(mat.getNativeObjAddr(), confThreshold, nmsThreshold);
    }

    private synchronized void initLibrary() {
        if (!isSOLoaded.get()) {
            Log.d("NcnnPredictorNative", "initLibrary: load yolov8ncnn");
            System.loadLibrary("yolov8ncnn");
            isSOLoaded.set(true);
        }
    }
}
