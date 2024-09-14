package com.stardust.autojs.core.image;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.util.Log;

import com.stardust.autojs.core.opencv.Mat;
import com.stardust.autojs.core.opencv.OpenCVHelper;
import com.stardust.pio.UncheckedIOException;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.RequiresApi;

/**
 * Created by Stardust on 2017/11/25.
 */
public class ImageWrapper {

    private Mat mMat;
    private final int mWidth;
    private final int mHeight;
    private Bitmap mBitmap;
    private static final ReentrantLock bitmapLock = new ReentrantLock();
    private static final String LOG_TAG = "ImageWrapper";

    protected ImageWrapper(Mat mat) {
        mMat = mat;
        mWidth = mat.cols();
        mHeight = mat.rows();
    }

    protected ImageWrapper(Bitmap bitmap) {
        mBitmap = bitmap;
        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();
    }

    protected ImageWrapper(Bitmap bitmap, Mat mat) {
        mBitmap = bitmap;
        mMat = mat;
        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();
    }

    public ImageWrapper(int width, int height) {
        this(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static ImageWrapper ofImage(Image image) {
        if (image == null) {
            return null;
        }
        if (OpenCVHelper.isInitialized()) {
            return ofImageByMat(image, CvType.CV_8UC4);
        } else {
            return new ImageWrapper(toBitmap(image));
        }
    }

    public static ImageWrapper ofMat(Mat mat) {
        if (mat == null) {
            return null;
        }
        return new ImageWrapper(mat);
    }

    public static ImageWrapper ofMat(org.opencv.core.Mat mat) {
        if (mat == null) {
            return null;
        }
        return new ImageWrapper(new Mat(mat.clone().getNativeObjAddr()));
    }

    public static ImageWrapper ofBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        return new ImageWrapper(bitmap);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static Bitmap toBitmap(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        buffer.position(0);
        int pixelStride = plane.getPixelStride();
        int rowPadding = plane.getRowStride() - pixelStride * image.getWidth();
        Bitmap bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        if (rowPadding == 0) {
            return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, image.getWidth(), image.getHeight());
    }

    public static ImageWrapper ofImageByMat(Image image, int cvType) {
        long start = System.currentTimeMillis();

        // 获取Image的平面
        Image.Plane[] planes = image.getPlanes();
//        Log.d("ImageWrapper", "ofImageByMat: planes.length: " + planes.length);
        // 获取Image的宽高
        int width = image.getWidth();
        int height = image.getHeight();
//        Log.d("ImageWrapper", "ofImageByMat: width:" + width + " height:" + height);

        Image.Plane plane = planes[0];
        // 获取平面的数据缓冲区
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        long s2 = System.currentTimeMillis();
        // 尽量避免使用临时数组
        Mat mat = new Mat(height, width + rowPadding / pixelStride, CvType.CV_8UC4);
        byte[] rowData = new byte[rowStride];
        for (int i = 0; i < height; i++) {
            buffer.get(rowData, 0, rowStride);
            mat.put(i, 0, rowData);
        }
//        Log.d("ImageWrapper", "ofImageByMat: create mat by bytes cost: " + (System.currentTimeMillis() - s2) + "ms");
        if (width != mat.width()) {
//            Log.d("ImageWrapper", "ofImageByMat: mat width is not valid: " + mat.width() + " => " + width);
            // 定义裁切区域
            Rect rect = new Rect(0, 0, width, height);
            Mat croppedImage = new Mat(mat, rect);
            mat.release();
            mat = croppedImage;
        }

        if (cvType != CvType.CV_8UC4) {
            long convertStart = System.currentTimeMillis();
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);
//            Log.d("ImageWrapper", "ofImageByMat: convert channel: " + (System.currentTimeMillis() - convertStart) + "ms");
        }

//        Log.d("ImageWrapper", "ofImageByMat: create by mat cost: " + (System.currentTimeMillis() - start) + "ms");
        return new ImageWrapper(mat);
    }

    public int getWidth() {
        ensureNotRecycled();
        return mWidth;
    }

    public int getHeight() {
        ensureNotRecycled();
        return mHeight;
    }

    public Mat getMat() {
        ensureNotRecycled();
        if (mMat == null && ensureBitmapNotRecycled()) {
            mMat = new Mat();
            Utils.bitmapToMat(mBitmap, mMat);
        }
        return mMat;
    }

    public void saveTo(String path) {
        ensureNotRecycled();
        if (mBitmap != null && !mBitmap.isRecycled()) {
            try {
                mBitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(path));
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            ensureMatNotRecycled();
            Imgcodecs.imwrite(path, mMat);
        }
    }

    public int pixel(int x, int y) {
        ensureNotRecycled();
        if (mBitmap != null && !mBitmap.isRecycled()) {
            return mBitmap.getPixel(x, y);
        }
        ensureMatNotRecycled();
        double[] channels = mMat.get(x, y);
        return Color.argb((int) channels[3], (int) channels[0], (int) channels[1], (int) channels[2]);
    }

    public Bitmap getBitmap() {
        ensureNotRecycled();
        if ((mBitmap == null || mBitmap.isRecycled()) && ensureMatNotRecycled()) {
            mBitmap = Bitmap.createBitmap(mMat.width(), mMat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mMat, mBitmap);
        }
        return mBitmap;
    }

    public void recycle() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            bitmapLock.lock();
            try {
                if (mBitmap != null && !mBitmap.isRecycled()) {
                    mBitmap.recycle();
                } else {
                    Log.w(LOG_TAG, "bitmap recycle 并发错误，已释放");
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "回收bitmap失败 " + e);
            } finally {
                mBitmap = null;
                bitmapLock.unlock();
            }
        } else if (mBitmap != null && mBitmap.isRecycled()) {
            Log.d(LOG_TAG, "recycle bitmap: not null but is recycled");
        }
        if (mMat != null) {
            OpenCVHelper.release(mMat);
            mMat = null;
        }

    }

    public void ensureNotRecycled() {
        if (mBitmap == null && mMat == null)
            throw new IllegalStateException("image has been recycled");
        if (mBitmap != null && mBitmap.isRecycled() && mMat != null && mMat.isReleased()) {
            Log.d(LOG_TAG, "ensureNotRecycled: bitmap and mat all recycled");
            throw new IllegalStateException("image has been recycled");
        }
    }

    public boolean ensureBitmapNotRecycled() {
        if (mBitmap == null || mBitmap.isRecycled())
            throw new IllegalStateException("image has been recycled");
        return true;
    }

    public boolean ensureMatNotRecycled() {
        if (mMat == null || mMat.isReleased())
            throw new IllegalStateException("image has been recycled");
        return true;
    }

    public boolean isRecycled() {
        if (mBitmap == null && mMat == null) {
            Log.d(LOG_TAG, "isRecycled: bitmap and mat all is null");
            return true;
        }
        if (mBitmap != null && mBitmap.isRecycled() && mMat != null && mMat.isReleased()) {
            Log.d(LOG_TAG, "isRecycled: bitmap and mat all recycled");
            return true;
        }
        return false;
    }

    public ImageWrapper clone() {
        ensureNotRecycled();
        if (mBitmap == null || mBitmap.isRecycled()) {
            return ImageWrapper.ofMat(mMat.clone());
        }
        if (mMat == null) {
            return ImageWrapper.ofBitmap(mBitmap.copy(mBitmap.getConfig(), true));
        }
        return new ImageWrapper(mBitmap.copy(mBitmap.getConfig(), true), mMat.clone());
    }
}
