package com.stardust.autojs.yolo;

import android.util.Log;

import com.stardust.autojs.yolo.onnx.domain.DetectResult;

import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;

/**
 * BaseYoloInstance类是一个实现了YoloInstance接口的具体类，用于封装YoloPredictor对象，
 * 并提供YOLO模型推理的核心功能，包括预测、设置阈值、检查初始化状态以及释放资源。
 *
 * @author TonyJiangWJ
 * @since 2025/1/5
 */
public class BaseYoloInstance extends YoloInstance {
    private final YoloPredictor predictor;

    /**
     * 构造函数，初始化BaseYoloInstance实例。
     *
     * @param predictor YoloPredictor对象，用于执行YOLO模型的推理操作。
     */
    public BaseYoloInstance(YoloPredictor predictor) {
        this.predictor = predictor;
    }

    /**
     * 获取当前实例的YoloPredictor对象。
     *
     * @return 返回封装的YoloPredictor对象。
     */
    @Override
    public YoloPredictor getPredictor() {
        return predictor;
    }

    /**
     * 对输入的图像进行YOLO模型推理，返回检测结果列表。
     *
     * @param image 输入的图像数据，类型为Mat（通常来自OpenCV）。
     * @return 返回检测结果列表，如果推理失败则返回空列表。
     */
    @Override
    public List<DetectResult> predictYolo(Mat image) {
        try {
            return predictor.predictYolo(image);
        } catch (Exception e) {
            Log.e("BaseYoloInstance", "predictYolo: failed", e);
            return Collections.emptyList();
        }
    }

    /**
     * 设置YOLO模型的置信度阈值。
     *
     * @param confThreshold 置信度阈值，范围通常为0到1。
     */
    @Override
    public void setConfThreshold(float confThreshold) {
        predictor.setConfThreshold(confThreshold);
    }

    /**
     * 设置YOLO模型的非极大值抑制（NMS）阈值。
     *
     * @param nmsThreshold NMS阈值，范围通常为0到1。
     */
    @Override
    public void setNmsThreshold(float nmsThreshold) {
        predictor.setNmsThreshold(nmsThreshold);
    }

    /**
     * 检查YOLO模型是否已经初始化。
     *
     * @return 如果模型已初始化，返回true；否则返回false。
     */
    @Override
    public boolean isInit() {
        return predictor.isInit();
    }

    /**
     * 释放YOLO模型占用的资源。
     */
    @Override
    public void release() {
        predictor.release();
    }
}