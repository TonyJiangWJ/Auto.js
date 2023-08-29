package com.stardust.autojs.onnx.domain;

/**
 * @author TonyJiangWJ
 * @since 2023/8/20
 * transfer from https://gitee.com/agricultureiot/yolo-onnx-java
 */
public class Detection {
    public String label;

    private Integer clsId;

    public float[] bbox;

    public float confidence;


    public Detection(String label,Integer clsId, float[] bbox, float confidence){
        this.clsId = clsId;
        this.label = label;
        this.bbox = bbox;
        this.confidence = confidence;
    }

    public Detection(){

    }

    public Integer getClsId() {
        return clsId;
    }

    public void setClsId(Integer clsId) {
        this.clsId = clsId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public float[] getBbox() {
        return bbox;
    }

    public void setBbox(float[] bbox) {
    }


    @Override
    public String toString() {
        return "  label="+label +
                " \t clsId="+clsId +
                " \t x0="+bbox[0] +
                " \t y0="+bbox[1] +
                " \t x1="+bbox[2] +
                " \t y1="+bbox[3] +
                " \t score="+confidence;
    }
}
