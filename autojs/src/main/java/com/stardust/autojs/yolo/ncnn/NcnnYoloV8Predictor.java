package com.stardust.autojs.yolo.ncnn;

import android.os.Build;
import android.util.Log;

import com.google.android.gms.common.util.CollectionUtils;
import com.stardust.autojs.yolo.YoloPredictor;
import com.stardust.autojs.yolo.onnx.domain.DetectResult;
import com.tony.yolov8ncnn.NcnnPredictorNative;
import com.tony.yolov8ncnn.PredictResult;

import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import androidx.annotation.RequiresApi;

/**
 * Ncnn YoloV8推理器
 *
 * @author TonyJiangWJ
 * @since 2024/6/1
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class NcnnYoloV8Predictor extends YoloPredictor {

    private String paramPath;
    private String binPath;

    private int shapeSize = 320;

    private boolean useGpu = false;

    private NcnnPredictorNative ncnnPredictorNative;


    public NcnnYoloV8Predictor(String paramPath, String binPath, List<String> labels) {
        this.paramPath = paramPath;
        this.binPath = binPath;
        this.labels = labels;
    }

    public boolean init() {
        ncnnPredictorNative = new NcnnPredictorNative();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        init = false;
        Thread thread = new Thread(() -> {
            init = ncnnPredictorNative.loadModel(paramPath, binPath, shapeSize, labels.size(), useGpu ? 1 : 0);
            countDownLatch.countDown();
        });
        thread.setName("Ncnn-init-thread");
        thread.start();
        try {
            // gpu初始化较慢
            if (!countDownLatch.await(useGpu ? 30 : 5, TimeUnit.SECONDS)) {
                // 其实线程无法终止 没救了
                thread.interrupt();
                throw new IllegalStateException("ncnn推理引擎初始化失败，请重启AutoJS。ncnn和paddle存在兼容性问题，请勿同时使用");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return init;
    }

    public List<DetectResult> predictYoloByPath(String imagePath) {
        if (!init) {
            return Collections.emptyList();
        }
        return doConvertResult(() -> ncnnPredictorNative.predictYoloByPath(imagePath, confThreshold, nmsThreshold));
    }

    private interface PredictExecutor {
        List<PredictResult> apply();
    }

    public List<DetectResult> predictYolo(Mat img) {
        Log.d("NcnnYoloV8Predictor", "predictYolo: image channels: " + img.channels());
        if (!init) {
            return Collections.emptyList();
        }
        return doConvertResult(() -> ncnnPredictorNative.predictYolo(img, confThreshold, nmsThreshold));
    }


    private List<DetectResult> doConvertResult(PredictExecutor executor) {
        long start_time = System.currentTimeMillis();
        AtomicReference<List<PredictResult>> predictResults = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            predictResults.set(executor.apply());
            countDownLatch.countDown();
        });
        thread.setName("Ncnn-doConvertResult-thread");
        thread.start();
        try {
            if (!countDownLatch.await(5, TimeUnit.SECONDS)) {
                Log.d("NcnnYoloV8Predictor", "doConvertResult: yolo predict timeout");
                // 其实线程无法终止 没救了
                thread.interrupt();
                throw new IllegalStateException("ncnn推理引擎存在问题，请重启AutoJS。ncnn和paddle存在兼容性问题，请勿同时使用");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Log.d("NcnnYoloV8Predictor", String.format("predict cost: %d ms", (System.currentTimeMillis() - start_time)));
        if (CollectionUtils.isEmpty(predictResults.get())) {
            return Collections.emptyList();
        }
        return predictResults.get().stream().map(predictResult -> {
            DetectResult detectResult = new DetectResult();
            detectResult.setConfidence(predictResult.getProb());
            detectResult.setClsId(predictResult.getLabel());
            if (labels.size() > predictResult.getLabel()) {
                detectResult.setLabel(labels.get(predictResult.getLabel()));
            } else {
                Log.e("NcnnYolov8Predictor", "predictYolo: label invalid " + predictResult.getLabel());
            }
            detectResult.setLeft(predictResult.getRect().x);
            detectResult.setTop(predictResult.getRect().y);
            detectResult.setRight(predictResult.getRect().x + predictResult.getRect().width);
            detectResult.setBottom(predictResult.getRect().y + predictResult.getRect().height);
            detectResult.buildRect();
            return detectResult;
        }).collect(Collectors.toList());
    }


    public String getParamPath() {
        return paramPath;
    }

    public void setParamPath(String paramPath) {
        this.paramPath = paramPath;
    }

    public String getBinPath() {
        return binPath;
    }

    public void setBinPath(String binPath) {
        this.binPath = binPath;
    }

    public int getShapeSize() {
        return shapeSize;
    }

    public void setShapeSize(int shapeSize) {
        this.shapeSize = shapeSize;
    }

    public boolean isUseGpu() {
        return useGpu;
    }

    public void setUseGpu(boolean useGpu) {
        this.useGpu = useGpu;
    }

    @Override
    public void release() {
        init = false;
        if (ncnnPredictorNative != null) {
            ncnnPredictorNative.release();
            ncnnPredictorNative = null;
        }
    }
}
