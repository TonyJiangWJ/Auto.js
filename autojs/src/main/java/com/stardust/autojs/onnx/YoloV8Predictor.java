package com.stardust.autojs.onnx;

import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.stardust.autojs.onnx.domain.DetectResult;
import com.stardust.autojs.onnx.domain.Detection;
import com.stardust.autojs.onnx.util.Letterbox;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.providers.NNAPIFlags;
import androidx.annotation.RequiresApi;

/**
 * @author TonyJiangWJ
 * @since 2023/8/20
 * transfer from https://gitee.com/agricultureiot/yolo-onnx-java
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class YoloV8Predictor {
    private static final String TAG = "YoloV8Predictor";

    static {
        OpenCVLoader.initDebug();
    }

    private final String modelPath;

    private float confThreshold = 0.35F;
    private float nmsThreshold = 0.55F;
    private boolean tryNpu;
    private Size shapeSize = new Size(640, 640);
    private Letterbox letterbox;

    private List<String> labels = new ArrayList<>();
    private List<String> apiFlags = Arrays.asList("CPU_DISABLED");

    public YoloV8Predictor(String modelPath) {
        this.modelPath = modelPath;
    }

    public YoloV8Predictor(String modelPath, float confThreshold, float nmsThreshold) {
        this.modelPath = modelPath;
        this.confThreshold = confThreshold;
        this.nmsThreshold = nmsThreshold;
    }

    public void setConfThreshold(float confThreshold) {
        this.confThreshold = confThreshold;
    }

    public void setNmsThreshold(float nmsThreshold) {
        this.nmsThreshold = nmsThreshold;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public void setShapeSize(double width, double height) {
        this.shapeSize = new Size(width, height);
    }

    public void setTryNpu(boolean tryNpu) {
        this.tryNpu = tryNpu;
    }

    public void setApiFlags(List<String> apiFlags) {
        this.apiFlags = apiFlags;
    }

    private OrtSession session;
    private OrtEnvironment environment;

    private void prepareSession() throws OrtException {
        if (environment != null) {
            return;
        }
        // 加载ONNX模型
        environment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
        addNNApiProvider(sessionOptions);

        session = environment.createSession(modelPath, sessionOptions);
        // 输出基本信息
        session.getInputInfo().keySet().forEach(x -> {
            try {
                System.out.println("input name = " + x);
                System.out.println(session.getInputInfo().get(x).getInfo().toString());
            } catch (OrtException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void addNNApiProvider(OrtSession.SessionOptions sessionOptions) {
        if (!tryNpu) {
            return;
        }
        try {
            List<NNAPIFlags> flags = new ArrayList<>();
            if (apiFlags.contains("USE_FP16")) {
                flags.add(NNAPIFlags.USE_FP16);
            }
            if (apiFlags.contains("USE_NCHW")) {
                flags.add(NNAPIFlags.USE_NCHW);
            }
            if (apiFlags.contains("CPU_ONLY")) {
                flags.add(NNAPIFlags.CPU_ONLY);
            }
            if (apiFlags.contains("CPU_DISABLED")) {
                flags.add(NNAPIFlags.CPU_DISABLED);
            }
            Log.d(TAG, "addNNApiProvider: 当前启用nnapiFlags:" + new Gson().toJson(apiFlags));
            sessionOptions.addNnapi(EnumSet.copyOf(flags));
            Log.d(TAG, "prepareSession: 启用nnapi成功");
        } catch (Exception e) {
            Log.e(TAG, "prepareSession: 无法启用nnapi");
        }
    }

    private HashMap<String, OnnxTensor> preprocessImage(Mat img) throws OrtException {

        // 读取 image
        Mat image = img.clone();
        // 将四通道转换为三通道
        if (image.channels() == 4) {
            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2BGR);
        }
        Log.d(TAG, "preprocessImage: image's channels: " + image.channels());
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGB);
        // 更改 image 尺寸
        letterbox = new Letterbox();
        letterbox.setNewShape(this.shapeSize);
        image = letterbox.letterbox(image);

        int rows = letterbox.getHeight();
        int cols = letterbox.getWidth();
        int channels = image.channels();

        // 将Mat对象的像素值赋值给Float[]对象
        float[] pixels = new float[channels * rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double[] pixel = image.get(j, i);
                for (int k = 0; k < channels; k++) {
                    // 这样设置相当于同时做了image.transpose((2, 0, 1))操作
                    pixels[rows * cols * k + j * cols + i] = (float) pixel[k] / 255.0f;
                }
            }
        }
        image.release();
        // 创建OnnxTensor对象
        long[] shape = {1L, (long) channels, (long) rows, (long) cols};
        OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(pixels), shape);
        HashMap<String, OnnxTensor> stringOnnxTensorHashMap = new HashMap<>();
        stringOnnxTensorHashMap.put(session.getInputInfo().keySet().iterator().next(), tensor);
        return stringOnnxTensorHashMap;
    }

    private List<Detection> postProcessOutput(OrtSession.Result output) throws OrtException {
        float[][] outputData = ((float[][][]) output.get(0).getValue())[0];

        outputData = transposeMatrix(outputData);
        Map<Integer, List<float[]>> class2Bbox = new HashMap<>();

        for (float[] bbox : outputData) {
            float[] conditionalProbabilities = Arrays.copyOfRange(bbox, 4, outputData.length);
            int label = argmax(conditionalProbabilities);
            float conf = conditionalProbabilities[label];
            if (conf < confThreshold) {
                continue;
            }

            bbox[4] = conf;

            // xywh to (x1, y1, x2, y2)
            xywh2xyxy(bbox);

            // skip invalid predictions
            if (bbox[0] >= bbox[2] || bbox[1] >= bbox[3]) {
                continue;
            }

            class2Bbox.putIfAbsent(label, new ArrayList<>());
            class2Bbox.get(label).add(bbox);
        }

        List<Detection> detections = new ArrayList<>();
        for (Map.Entry<Integer, List<float[]>> entry : class2Bbox.entrySet()) {
            int label = entry.getKey();
            List<float[]> bboxes = entry.getValue();
            bboxes = nonMaxSuppression(bboxes, nmsThreshold);
            for (float[] bbox : bboxes) {
                String labelString = "";
                if (labels.size() - 1 < label) {
                    labelString = String.valueOf(label);
                } else {
                    labelString = labels.get(label);
                }
                detections.add(new Detection(labelString, entry.getKey(), Arrays.copyOfRange(bbox, 0, 4), bbox[4]));
            }
        }
        return detections;
    }

    public List<DetectResult> predictYolo(String imagePath) throws OrtException {
        return predictYolo(Imgcodecs.imread(imagePath));
    }

    public List<DetectResult> predictYolo(Mat image) throws OrtException {
        prepareSession();
        long start_time = System.currentTimeMillis();
        // 运行推理
        OrtSession.Result output = session.run(preprocessImage(image));
        List<Detection> detections = postProcessOutput(output);
        System.out.printf("time：%d ms.\n", (System.currentTimeMillis() - start_time));
        return detections.stream().map(detection -> new DetectResult(detection, letterbox))
                .collect(Collectors.toList());
    }

    public static void xywh2xyxy(float[] bbox) {
        float x = bbox[0];
        float y = bbox[1];
        float w = bbox[2];
        float h = bbox[3];

        bbox[0] = x - w * 0.5f;
        bbox[1] = y - h * 0.5f;
        bbox[2] = x + w * 0.5f;
        bbox[3] = y + h * 0.5f;
    }

    public static float[][] transposeMatrix(float[][] m) {
        float[][] temp = new float[m[0].length][m.length];
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                temp[j][i] = m[i][j];
            }
        }
        return temp;
    }

    public static List<float[]> nonMaxSuppression(List<float[]> bboxes, float iouThreshold) {

        List<float[]> bestBboxes = new ArrayList<>();

        bboxes.sort(Comparator.comparing(a -> a[4]));

        while (!bboxes.isEmpty()) {
            float[] bestBbox = bboxes.remove(bboxes.size() - 1);
            bestBboxes.add(bestBbox);
            bboxes = bboxes.stream().filter(a -> computeIOU(a, bestBbox) < iouThreshold).collect(Collectors.toList());
        }

        return bestBboxes;
    }

    public static float computeIOU(float[] box1, float[] box2) {

        float area1 = (box1[2] - box1[0]) * (box1[3] - box1[1]);
        float area2 = (box2[2] - box2[0]) * (box2[3] - box2[1]);

        float left = Math.max(box1[0], box2[0]);
        float top = Math.max(box1[1], box2[1]);
        float right = Math.min(box1[2], box2[2]);
        float bottom = Math.min(box1[3], box2[3]);

        float interArea = Math.max(right - left, 0) * Math.max(bottom - top, 0);
        float unionArea = area1 + area2 - interArea;
        return Math.max(interArea / unionArea, 1e-8f);

    }

    //返回最大值的索引
    public static int argmax(float[] a) {
        float re = -Float.MAX_VALUE;
        int arg = -1;
        for (int i = 0; i < a.length; i++) {
            if (a[i] >= re) {
                re = a[i];
                arg = i;
            }
        }
        return arg;
    }
}
