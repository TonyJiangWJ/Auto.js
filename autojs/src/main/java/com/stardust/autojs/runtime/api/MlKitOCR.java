package com.stardust.autojs.runtime.api;

import android.graphics.Bitmap;
import android.util.Log;

import com.baidu.paddle.lite.ocr.OcrResult;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.stardust.autojs.core.image.ImageWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MlKitOCR {
    private static final String TAG = "MlKitOCR";
    private TextRecognizer recognizer;

    public void initIfNeeded() {
        if (recognizer == null) {
            recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        }
    }

    public void release() {
        if (recognizer != null) {
            recognizer.close();
        }
    }

    public List<OcrResult> detect(ImageWrapper image) {
        initIfNeeded();
        if (image == null) {
            return Collections.emptyList();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return Collections.emptyList();
        }
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        Task<Text> result = recognizer.process(inputImage)
                .addOnSuccessListener(visionText -> {
                    // Task completed successfully
                    synchronized (TAG) {
                        TAG.notify();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "detect: failed " + e.getMessage());
                    e.printStackTrace();
                    // Task failed with an exception
                    synchronized (TAG) {
                        TAG.notify();
                    }
                });
        while (!result.isComplete()) {
            // wait
            synchronized (TAG) {
                try {
                    TAG.wait(50);
                } catch (InterruptedException e) {
                    //
                }
            }
        }
        if (!result.isSuccessful()) {
            Log.d(TAG, "detect failed");
            return Collections.emptyList();
        }
        Text text = result.getResult();
        List<OcrResult> ocrResults = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                OcrResult lineResult = new OcrResult(line.getText(), line.getConfidence(), line.getBoundingBox());
                for (Text.Element element : line.getElements()) {
                    lineResult.addElements(new OcrResult(element.getText(), element.getConfidence(), element.getBoundingBox()));
                }
                ocrResults.add(lineResult);
            }
        }
        return ocrResults;
    }

    public String[] recognizeText(ImageWrapper image) {

        initIfNeeded();
        List<OcrResult> words_result = detect(image);
        Collections.sort(words_result);
        String[] outputResult = new String[words_result.size()];
        for (int i = 0; i < words_result.size(); i++) {
            outputResult[i] = words_result.get(i).getLabel();
            Log.i("outputResult", outputResult[i]); // show LOG in Logcat panel
        }
        return outputResult;
    }

}



