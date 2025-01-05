package com.stardust.autojs.onnx;

import android.os.Build;

import com.stardust.autojs.yolo.onnx.OnnxYoloV8Predictor;

import androidx.annotation.RequiresApi;

/**
 * @author TonyJiangWJ
 * @since 2023/8/20
 * 适配旧版本脚本
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class YoloV8Predictor extends OnnxYoloV8Predictor {

    public YoloV8Predictor(String modelPath) {
        super(modelPath);
    }

    public YoloV8Predictor(String modelPath, float confThreshold, float nmsThreshold) {
        super(modelPath, confThreshold, nmsThreshold);
    }

}
