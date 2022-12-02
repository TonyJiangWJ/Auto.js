package com.tony.autojs.search;

import android.util.Log;

import com.stardust.automator.UiGlobalSelector;
import com.stardust.automator.search.SearchAlgorithm;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AlgorithmChanger {

    protected final static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 4, 60, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>()
            , new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName("parallel-pre-build-t-" + t.getName());
            return t;
        }
    });

    private static final String TAG = "AlgorithmChanger";
    public static boolean enableLogging = false;

    public static UiGlobalSelector changeAlgorithm(UiGlobalSelector selector, String algorithm) {
        SearchAlgorithm realAlgorithm = getAlgorithm(algorithm);
        if (realAlgorithm != null) {
            return doChange(selector, realAlgorithm);
        }
        return selector.algorithm(algorithm);
    }

    private static SearchAlgorithm getAlgorithm(String algorithm) {
        switch (algorithm) {
            case "VBFS":
                return new VisibleBFS();
            case "VDFS":
                return new VisibleDFS();
            case "PVBFS":
                return new ParallelVisibleBFS();
            case "PVDFS":
                return new ParallelVisibleDFS();
            case "PDFS":
                return new ParallelDFS();
            case "PBFS":
                return new ParallelBFS();
            default:
                return null;
        }
    }

    private static UiGlobalSelector doChange(UiGlobalSelector selector, SearchAlgorithm searchAlgorithm) {
        try {
            Field field = UiGlobalSelector.class.getDeclaredField("mSearchAlgorithm");
            field.setAccessible(true);
            Object current = field.get(selector);
            if (current instanceof SearchAlgorithm) {
                Log.d(TAG, "当前算法：" + current.getClass());
            }
            field.set(selector, searchAlgorithm);
            return selector;
        } catch (Exception e) {
            Log.d(TAG, "修改搜索算法失败: " + e);
            throw new IllegalStateException("替换失败" + e);
        }
    }

    public static String getCurrentAlgorithm(UiGlobalSelector selector) {
        try {
            Field field = UiGlobalSelector.class.getDeclaredField("mSearchAlgorithm");
            field.setAccessible(true);
            Object current = field.get(selector);
            if (current instanceof SearchAlgorithm) {
                return current.getClass().getName();
            }
            return "UNKNOWN";
        } catch (Exception e) {
            Log.d(TAG, "获取搜索算法失败: " + e);
            return "UNKNOWN ERROR";
        }
    }

    public static void enableLogging() {
        enableLogging = true;
    }

    public static void disableLogging() {
        enableLogging = false;
    }
}
