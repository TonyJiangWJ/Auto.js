package com.stardust.autojs.yolo;

/**
 * yolo实例抽象工厂，用于创建不同类型的yolo实例 目前支持ncnn和onnx的yolov8版本实例
 *
 * @param <P> 模型初始化参数
 * @author TonyJiangWJ
 * @since 2025/1/5
 */
public interface YoloInstanceFactory<P extends ModelInitParams> {
    /**
     * 创建yolo实例
     *
     * @param initParams 初始化参数
     * @return 返回yolo实例
     */
    YoloInstance createInstance(P initParams);

}
