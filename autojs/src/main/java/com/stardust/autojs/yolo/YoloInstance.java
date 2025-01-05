package com.stardust.autojs.yolo;


import android.media.Image;

import com.stardust.autojs.core.image.ImageWrapper;
import com.stardust.autojs.runtime.ScriptRuntime;
import com.stardust.autojs.runtime.api.Images;
import com.stardust.autojs.yolo.onnx.domain.DetectResult;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.Collections;
import java.util.List;

/**
 * YoloInstance是一个抽象类，定义了YOLO实例的基本行为和功能。
 * 该类提供了YOLO模型推理的核心方法，包括预测、设置阈值、检查初始化状态、释放资源以及捕获屏幕并预测的功能。
 *
 * @author TonyJiangWJ
 * @since 2025/1/5
 */
public abstract class YoloInstance {

    /**
     * 获取当前实例的YoloPredictor对象。
     *
     * @return 返回封装的YoloPredictor对象。
     */
    public abstract YoloPredictor getPredictor();

    /**
     * 对输入的图像进行YOLO模型推理，返回检测结果列表。
     *
     * @param image 输入的图像数据，类型为Mat（通常来自OpenCV）。
     * @return 返回检测结果列表。
     */
    public abstract List<DetectResult> predictYolo(Mat image);

    /**
     * 设置YOLO模型的置信度阈值。
     *
     * @param confThreshold 置信度阈值，范围通常为0到1。
     */
    public void setConfThreshold(float confThreshold) {
        getPredictor().setConfThreshold(confThreshold);
    }

    /**
     * 设置YOLO模型的非极大值抑制（NMS）阈值。
     *
     * @param nmsThreshold NMS阈值，范围通常为0到1。
     */
    public void setNmsThreshold(float nmsThreshold) {
        getPredictor().setNmsThreshold(nmsThreshold);
    }

    /**
     * 检查YOLO模型是否已经初始化。
     *
     * @return 如果模型已初始化，返回true；否则返回false。
     */
    public boolean isInit() {
        return getPredictor().isInit();
    }

    /**
     * 释放YOLO模型占用的资源。
     */
    public void release() {
        getPredictor().release();
    }

    /**
     * 捕获屏幕图像并进行YOLO模型推理。
     *
     * @param runtime 脚本运行时环境，用于获取图像捕获功能。
     * @param rect    指定捕获屏幕的区域，如果为null则捕获整个屏幕。
     * @return 返回检测结果列表，如果捕获或推理失败则返回空列表。
     */
    public List<DetectResult> captureAndPredict(ScriptRuntime runtime, Rect rect) {
        Images images = (Images) runtime.getImages();
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
