package com.tony.yolov8ncnn;

import org.opencv.core.Rect;

public class PredictResult {
    private Rect rect;
    private int label;
    private float prob;

    public PredictResult(Rect rect, int label, float prob) {
        this.rect = rect;
        this.label = label;
        this.prob = prob;
    }

    public PredictResult() {
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public int getLabel() {
        return label;
    }

    public void setLabel(int label) {
        this.label = label;
    }

    public float getProb() {
        return prob;
    }

    public void setProb(float prob) {
        this.prob = prob;
    }

    @Override
    public String toString() {
        return "PredictResult{" +
                "rect=" + rect +
                ", label=" + label +
                ", prob=" + prob +
                '}';
    }
}
