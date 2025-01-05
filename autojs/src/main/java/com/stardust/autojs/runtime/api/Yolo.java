package com.stardust.autojs.runtime.api;

import android.os.Build;

import com.stardust.autojs.yolo.ModelInitParams;
import com.stardust.autojs.yolo.YoloInstance;
import com.stardust.autojs.yolo.ncnn.NcnnInitParams;
import com.stardust.autojs.yolo.ncnn.NcnnYoloInstanceFactory;
import com.stardust.autojs.yolo.onnx.OnnxYoloInstanceFactory;

import java.util.List;

import androidx.annotation.RequiresApi;

/**
 * @author TonyJiangWJ
 * @since 2024/6/1
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class Yolo {
    private static final String TAG = "Yolo";

    private final NcnnYoloInstanceFactory ncnnFactory = new NcnnYoloInstanceFactory();
    private final OnnxYoloInstanceFactory onnxFactory = new OnnxYoloInstanceFactory();

    public YoloInstance createOnnx(String modelPath, List<String> labels, Integer imageSize) {
        ModelInitParams params = new ModelInitParams();
        params.setModelPath(modelPath);
        params.setLabels(labels);
        params.setImageSize(imageSize);
        return onnxFactory.createInstance(params);
    }


    public YoloInstance createNcnn(String paramPath, String binPath, List<String> labels, Integer imageSize, boolean useGpu) {
        NcnnInitParams params = new NcnnInitParams();
        params.setParamPath(paramPath);
        params.setBinPath(binPath);
        params.setLabels(labels);
        params.setImageSize(imageSize);
        params.setUseGpu(useGpu);
        return ncnnFactory.createInstance(params);
    }

}
