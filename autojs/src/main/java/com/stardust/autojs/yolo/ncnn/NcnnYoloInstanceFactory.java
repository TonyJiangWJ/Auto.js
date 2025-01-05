package com.stardust.autojs.yolo.ncnn;

import android.os.Build;
import android.util.Log;

import com.stardust.autojs.yolo.BaseYoloInstance;
import com.stardust.autojs.yolo.YoloInstance;
import com.stardust.autojs.yolo.YoloInstanceFactory;

import androidx.annotation.RequiresApi;

public class NcnnYoloInstanceFactory implements YoloInstanceFactory<NcnnInitParams> {

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public YoloInstance createInstance(NcnnInitParams initParams) {
        NcnnYoloV8Predictor predictor = new NcnnYoloV8Predictor(initParams.getParamPath(),
                initParams.getBinPath(),
                initParams.getLabels());
        predictor.setShapeSize(initParams.getImageSize());
        predictor.setUseGpu(initParams.isUseGpu());
        Log.d("NcnnYoloInstanceFactory", "ncnnYoloV8 instance initializer: " + predictor.init());
        return new BaseYoloInstance(predictor);
    }

}

