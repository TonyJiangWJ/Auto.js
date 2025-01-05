package com.stardust.autojs.yolo.onnx.util;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * @author TonyJiangWJ
 * @since 2023/8/20
 * transfer from <a href="https://gitee.com/agricultureiot/yolo-onnx-java">yolo-onnx-java</a>
 */
public class Letterbox {

    private Size newShape = new Size(640, 640);
    private final double[] color = new double[]{114, 114, 114};
    private final Boolean auto = false;
    private final Boolean scaleUp = true;
    private Integer stride = 32;

    private double ratio;
    private double dw;
    private double dh;

    public double getRatio() {
        return ratio;
    }

    public double getDw() {
        return dw;
    }

    public Integer getWidth() {
        return (int) this.newShape.width;
    }

    public Integer getHeight() {
        return (int) this.newShape.height;
    }

    public double getDh() {
        return dh;
    }

    public void setNewShape(Size newShape) {
        this.newShape = newShape;
    }

    public void setStride(Integer stride) {
        this.stride = stride;
    }

    public Mat letterbox(Mat im) { // 调整图像大小和填充图像，使满足步长约束，并记录参数

        // 当前形状 [height, width]
        int[] shape = {im.rows(), im.cols()};
        // Scale ratio (new / old)
        double r = Math.min(this.newShape.height / shape[0], this.newShape.width / shape[1]);
        // 仅缩小，不扩大（一且为了mAP）
        if (!this.scaleUp) {
            r = Math.min(r, 1.0);
        }
        // Compute padding
        Size newUnPad = new Size(Math.round(shape[1] * r), Math.round(shape[0] * r));
        // wh 填充
        double dw = this.newShape.width - newUnPad.width, dh = this.newShape.height - newUnPad.height;
        // 最小矩形
        if (this.auto) {
            dw = dw % this.stride;
            dh = dh % this.stride;
        }
        // 填充的时候两边都填充一半，使图像居于中心
        dw /= 2;
        dh /= 2;
        // resize
        if (shape[1] != newUnPad.width || shape[0] != newUnPad.height) {
            Imgproc.resize(im, im, newUnPad, 0, 0, Imgproc.INTER_LINEAR);
        }
        int top = (int) Math.round(dh - 0.1), bottom = (int) Math.round(dh + 0.1);
        int left = (int) Math.round(dw - 0.1), right = (int) Math.round(dw + 0.1);
        // 将图像填充为正方形
        Core.copyMakeBorder(im, im, top, bottom, left, right, Core.BORDER_CONSTANT, new org.opencv.core.Scalar(this.color));
        this.ratio = r;
        this.dh = dh;
        this.dw = dw;
        return im;
    }
}