// SPDX-License-Identifier: GPL-3.0
// 此代码属全新文件，除上面一行所声明的协议（商业使用需要另外授权），无需遵循原autojs项目协议（MPL2.0及非商业性使用条款 ）
package com.stardust.autojs.runtime.api;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.Locale;
import java.util.UUID;


public class Speech extends UtteranceProgressListener {

    private final Context mContext;
    private TextToSpeech mTextToSpeech;
    private boolean initSuccess = false;
    private boolean support = true;
    private String requestEngine;
    private final String TAG = "Speech";

    private static final ResultHandler<Void> NoOpHandler = r -> {
    };
    private static final InitCallback<Void> NoOpCallback = () -> null;

    public Speech(Context context) {
        this.mContext = context.getApplicationContext();
    }

    private <R> void initSpeech(InitCallback<R> initCallback, ResultHandler<R> resultHandler) {
        if (initSuccess) {
            Log.d(TAG, "initSpeech: 已初始化speech，触发重新初始化");
            destroy();
        }
        mTextToSpeech = new TextToSpeech(mContext, status -> {
            Log.d(TAG, "initSpeech: callback on init speech " + status);
            // TTS初始化
            if (status == TextToSpeech.SUCCESS) {
                initSuccess = true;
                int result = mTextToSpeech.setLanguage(Locale.CHINA);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = mTextToSpeech.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        initSuccess = false;
                        support = false;
                    }
                }
            } else {
                Log.e(TAG, "初始化TTS失败，可能不支持");
                support = false;
            }
            if (initSuccess) {
                resultHandler.resolve(initCallback.onInitSuccess());
            } else {
                resultHandler.resolve(null);
                Log.e(TAG, "初始化TTS失败，可能不支持");
            }
        }, requestEngine);
    }

    public void setEngine(String engine) {
        this.requestEngine = engine;
        if (initSuccess) {
            initSpeech(NoOpCallback, NoOpHandler);
        }
    }

    public void setEngine(String engine, ResultHandler<Boolean> resultHandler) {
        this.requestEngine = engine;
        runActionOnInit(() -> initSuccess, resultHandler);
    }

    private String generateUtteranceId() {
        return UUID.randomUUID().toString();
    }

    public boolean checkInitStatus() {
        if (!initSuccess) {
            Toast.makeText(mContext, "TTS未初始化或者初始化失败", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "checkInitStatus: TTS未初始化或者初始化失败 support:" + support);
            if (support) {
                initSpeech(NoOpCallback, NoOpHandler);
            }
        }
        return initSuccess;
    }

    public String[] getEngines() {
        if (checkInitStatus()) {
            String[] result = new String[mTextToSpeech.getEngines().toArray().length];
            int i = 0;
            for (TextToSpeech.EngineInfo item : mTextToSpeech.getEngines()) {
                result[i++] = item.name;
            }
            return result;
        }
        return new String[0];
    }

    public String[] getVoices() {
        if (checkInitStatus()) {
            String[] result = new String[mTextToSpeech.getVoices().toArray().length];
            int i = 0;
            for (Voice item : mTextToSpeech.getVoices()) {
                result[i++] = item.getName();
            }
            return result;
        }
        return new String[0];
    }

    public void setVoice(String voiceName, ResultHandler<Integer> handler) {
        runActionOnInit(() -> {
            for (Voice item : mTextToSpeech.getVoices()) {
                if (voiceName.equals(item.getName())) {
                    return mTextToSpeech.setVoice(item);
                }
            }
            return TextToSpeech.ERROR;
        }, handler);
    }

    public String[] getLanguages() {
        if (checkInitStatus()) {
            String[] result = new String[mTextToSpeech.getAvailableLanguages().toArray().length];
            int i = 0;
            for (Locale item : mTextToSpeech.getAvailableLanguages()) {
                result[i++] = item.getDisplayName();
            }
            return result;
        }
        return new String[0];
    }

    public void setLanguage(String language, ResultHandler<Integer> handler) {
        runActionOnInit(() -> {
            for (Locale item : mTextToSpeech.getAvailableLanguages()) {
                if (language.equals(item.getDisplayName())) {
                    return mTextToSpeech.setLanguage(item);
                }
            }
            return TextToSpeech.ERROR;
        }, handler);
    }

    public void speak(String text, Float pitch, Float speechRate, Float volume) {
        runActionOnInit(() -> {
            mTextToSpeech.setPitch(pitch);// 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
            mTextToSpeech.setSpeechRate(speechRate);
            //设置音量
            Bundle bundle = new Bundle();
            bundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_ADD, bundle, generateUtteranceId());
            return null;
        });
    }

    public void speak(String text) {
        speak(text, 1.0f, 1.0f, 0.8f);
    }

    public void synthesizeToFile(String text, Float pitch, Float speechRate, Float volume, String fileName) {
        runActionOnInit(() -> {
            mTextToSpeech.setPitch(pitch);// 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
            mTextToSpeech.setSpeechRate(speechRate);
            // 设置音量
            Bundle bundle = new Bundle();
            bundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
            mTextToSpeech.synthesizeToFile(text, bundle, new File(fileName), generateUtteranceId());
            return null;
        });
    }

    private interface InitCallback<T> {
        T onInitSuccess();
    }

    public interface ResultHandler<R> {
        void resolve(R result);
    }



    private <R> void runActionOnInit(InitCallback<R> callback) {
        runActionOnInit(callback, result -> { });
    }

    private <R> void runActionOnInit(InitCallback<R> callback, ResultHandler<R> handler) {
        if (initSuccess) {
            handler.resolve(callback.onInitSuccess());
        } else if (support) {
            initSpeech(callback, handler);
        } else {
            Toast.makeText(mContext, "TTS初始化失败", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "runActionOnInit: TTS初始化失败");
        }
    }

    public boolean isSpeaking() {
        if (!checkInitStatus()) {
            return false;
        }
        return mTextToSpeech.isSpeaking();
    }

    public void shutdown() {
        if (mTextToSpeech != null) {
            mTextToSpeech.shutdown();
            mTextToSpeech = null;
        }
        initSuccess = false;
    }

    public void stop() {
        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
        }
    }

    public void destroy() {
        stop();
        shutdown();
    }

    @Override
    public void onStart(String utteranceId) {
        Log.d(TAG, "onStart: " + utteranceId);
    }

    @Override
    public void onDone(String utteranceId) {
        Log.d(TAG, "onDone: " + utteranceId);
    }

    @Override
    public void onError(String utteranceId) {
        Log.d(TAG, "onError: " + utteranceId);
    }
}