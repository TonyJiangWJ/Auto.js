package com.stardust.autojs.yolo.onnx;


import android.os.Build;

import com.stardust.autojs.yolo.BaseYoloInstance;
import com.stardust.autojs.yolo.ModelInitParams;
import com.stardust.autojs.yolo.YoloInstance;
import com.stardust.autojs.yolo.YoloInstanceFactory;

import androidx.annotation.RequiresApi;

/**
 * OnnxYoloV8实例创建工厂
 *
 * @author TonyJiangWJ
 * @since 2025/1/5
 */
public class OnnxYoloInstanceFactory implements YoloInstanceFactory<ModelInitParams> {
    /**
     * 创建YoloInstance实例
     *
     * @param modelInitParams 初始化参数
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public YoloInstance createInstance(ModelInitParams modelInitParams) {
        OnnxYoloV8Predictor predictor = new OnnxYoloV8Predictor(modelInitParams.getModelPath());
        predictor.setLabels(modelInitParams.getLabels());
        predictor.setShapeSize(modelInitParams.getImageSize(), modelInitParams.getImageSize());
        return new BaseYoloInstance(predictor);
    }
}
