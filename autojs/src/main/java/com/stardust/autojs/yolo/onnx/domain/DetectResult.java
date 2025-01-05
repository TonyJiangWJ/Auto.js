package com.stardust.autojs.yolo.onnx.domain;

import android.graphics.Rect;

import com.stardust.autojs.yolo.onnx.util.Letterbox;

/**
 * @author TonyJiangWJ
 * @since 2023/8/20
 * transfer from <a href="https://gitee.com/agricultureiot/yolo-onnx-java">yolo-onnx-java</a>
 */
public class DetectResult {

    private String label;
    private Integer clsId;
    private double left;
    private double top;
    private double right;
    private double bottom;

    private Rect bounds;

    private float confidence;

    public DetectResult() {
    }

    public DetectResult(Detection detection, Letterbox letterbox) {
        this.label = detection.label;
        this.clsId = detection.getClsId();
        this.confidence = detection.confidence;
        double dw = letterbox.getDw();
        double dh = letterbox.getDh();
        double ratio = letterbox.getRatio();
        left = (detection.getBbox()[0] - dw) / ratio;
        right = (detection.getBbox()[2] - dw) / ratio;
        top = (detection.getBbox()[1] - dh) / ratio;
        bottom = (detection.getBbox()[3] - dh) / ratio;
        bounds = new Rect((int) left, (int) top, (int) right, (int) bottom);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getClsId() {
        return clsId;
    }

    public void setClsId(Integer clsId) {
        this.clsId = clsId;
    }

    public double getLeft() {
        return left;
    }

    public void setLeft(double left) {
        this.left = left;
    }

    public double getTop() {
        return top;
    }

    public void setTop(double top) {
        this.top = top;
    }

    public double getRight() {
        return right;
    }

    public void setRight(double right) {
        this.right = right;
    }

    public double getBottom() {
        return bottom;
    }

    public void setBottom(double bottom) {
        this.bottom = bottom;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public Rect getBounds() {
        return bounds;
    }

    public void setBounds(Rect bounds) {
        this.bounds = bounds;
    }

    public void buildRect() {
        bounds = new Rect((int) left, (int) top, (int) right, (int) bottom);
    }
}
