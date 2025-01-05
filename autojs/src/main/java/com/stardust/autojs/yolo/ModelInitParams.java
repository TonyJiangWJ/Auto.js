package com.stardust.autojs.yolo;

import java.util.List;


/**
 * 用于存储模型初始化所需的参数
 *
 * @author TonyJiangWJ
 * @since 2025/1/5
 */
public class ModelInitParams {
    private String modelPath;
    private List<String> labels;
    private Integer imageSize;

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public Integer getImageSize() {
        return imageSize;
    }

    public void setImageSize(Integer imageSize) {
        this.imageSize = imageSize;
    }
}
