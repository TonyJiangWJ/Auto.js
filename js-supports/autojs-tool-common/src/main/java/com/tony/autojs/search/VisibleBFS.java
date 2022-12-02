package com.tony.autojs.search;

import android.util.Log;

import com.stardust.automator.UiObject;
import com.stardust.automator.filter.Filter;
import com.stardust.automator.search.SearchAlgorithm;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

import androidx.annotation.NonNull;

public class VisibleBFS implements SearchAlgorithm {
    private static final String TAG = "VisibleBFS";

    @NonNull
    @Override
    public ArrayList<UiObject> search(@NonNull UiObject root, @NonNull Filter filter, int limit) {
        Log.d(TAG, "search limit: " + limit);
        ArrayList<UiObject> result = new ArrayList<>();
        Deque<UiObject> queue = new LinkedList<>();
        queue.add(root);
        LoopExecutor executor = createExecutor();
        while (!queue.isEmpty()) {
            UiObject top = queue.poll();
            if (top == null) {
                continue;
            }
            boolean isTarget = filter.filter(top);
            if (isTarget) {
                result.add(top);
                if (result.size() >= limit) {
                    return result;
                }
            }
            for (int i = 0; i < top.childCount(); i++) {
                executor.executeLoop(queue, top.child(i), i);
            }
            if (!isTarget && top != root) {
                top.recycle();
            }
        }
        return result;
    }

    private LoopExecutor createExecutor() {
        // dx不支持函数式 需要写成匿名内部类
        return AlgorithmChanger.enableLogging
                ? new LoopExecutor() {
            @Override
            public void executeLoop(Queue<UiObject> queue, UiObject child, int i) {
                executeLoopWithLog(queue, child, i);
            }
        } : new LoopExecutor() {
            @Override
            public void executeLoop(Queue<UiObject> queue, UiObject child, int i) {
                executeLoopWithoutLog(queue, child, i);
            }
        };
    }

    private interface LoopExecutor {
        void executeLoop(Queue<UiObject> queue, UiObject child, int i);
    }

    private void executeLoopWithLog(Queue<UiObject> queue, UiObject child, int i) {
        if (child != null) {
            if (child.visibleToUser()) {
                Log.d(TAG, "child " + i + "is visible depth:" + child.depth() + "id[" + child.id() + "] bounds:" + child.bounds());
                queue.add(child);
            } else {
                Log.d(TAG, "child not visible drop: id[" + child.id() + "] bounds:" + child.bounds());
                child.recycle();
            }
        }
    }

    private void executeLoopWithoutLog(Queue<UiObject> queue, UiObject child, int i) {
        if (child != null) {
            if (child.visibleToUser()) {
                queue.add(child);
            } else {
                child.recycle();
            }
        }
    }
}
