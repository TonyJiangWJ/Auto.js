package com.stardust.autojs.runtime.api;

import android.media.Image;
import android.os.Build;
import android.util.Log;

import com.stardust.autojs.core.image.ImageWrapper;
import com.stardust.autojs.ncnn.NcnnYoloV8Predictor;
import com.stardust.autojs.onnx.YoloV8Predictor;
import com.stardust.autojs.onnx.domain.DetectResult;
import com.stardust.autojs.runtime.ScriptRuntime;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.Collections;
import java.util.List;

import ai.onnxruntime.OrtException;
import androidx.annotation.RequiresApi;

/**
 * @author TonyJiangWJ
 * @since 2024/6/1
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class Yolo {
    private static final String TAG = "Yolo";

    public YoloInstance createNcnn(String paramPath, String binPath, List<String> labels, Integer imageSize, boolean useGpu) {
        return new YoloInstance() {

            private NcnnYoloV8Predictor ncnnYoloV8 = new NcnnYoloV8Predictor(paramPath, binPath, labels);

            {
                ncnnYoloV8.setShapeSize(imageSize);
                ncnnYoloV8.setUseGpu(useGpu);
                Log.d(TAG, "ncnnYoloV8 instance initializer: " + ncnnYoloV8.init());
            }

            @Override
            public YoloPredictor getPredictor() {
                return ncnnYoloV8;
            }

            @Override
            public List<DetectResult> predictYolo(Mat image) {
                return ncnnYoloV8.predictYolo(image);
            }
        };
    }

    public YoloInstance createOnnx(String modelPath, List<String> labels, Integer imageSize) {
        return new YoloInstance() {
            private YoloV8Predictor onnxYoloV8 = new YoloV8Predictor(modelPath);

            {
                onnxYoloV8.setLabels(labels);
                onnxYoloV8.setShapeSize(imageSize, imageSize);
            }

            @Override
            public YoloPredictor getPredictor() {
                return onnxYoloV8;
            }

            @Override
            public List<DetectResult> predictYolo(Mat image) {
                try {
                    return onnxYoloV8.predictYolo(image);
                } catch (OrtException e) {
                    return Collections.emptyList();
                }
            }
        };
    }


    public static abstract class YoloInstance {

        public abstract YoloPredictor getPredictor();

        public abstract List<DetectResult> predictYolo(Mat image);

        public void setConfThreshold(float confThreshold) {
            getPredictor().setConfThreshold(confThreshold);
        }

        public void setNmsThreshold(float nmsThreshold) {
            getPredictor().setNmsThreshold(nmsThreshold);
        }

        public boolean isInit() {
            return getPredictor().isInit();
        }

        public void release() {
            getPredictor().release();
        }

        public List<DetectResult> captureAndPredict(ScriptRuntime runtime, Rect rect) {
            Images images = (Images)runtime.getImages();
            Image image = images.captureScreenRaw();
            if (image != null) {
                ImageWrapper imageWrapper = ImageWrapper.ofImageByMat(image, CvType.CV_8UC4);
                image.close();
                Mat mat = imageWrapper.getMat();
                if (rect != null) {
                    // 裁切图像
                    Mat croppedImage = new Mat(mat, rect);
                    mat.release();
                    mat = croppedImage;
                }
                List<DetectResult> results = this.predictYolo(mat);
                mat.release();
                return results;
            }
            return Collections.emptyList();
        }

    }
}
