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

public class VisibleDFS implements SearchAlgorithm {
    private static final String TAG = "VisibleDFS";

    @NonNull
    @Override
    public ArrayList<UiObject> search(@NonNull UiObject root, @NonNull Filter filter, int limit) {
        Log.d(TAG, "search limit: " + limit);
        ArrayList<UiObject> result = new ArrayList<>();
        Deque<UiObject> stack = new LinkedList<>();
        stack.push(root);
        LoopExecutor executor = createExecutor();
        while (!stack.isEmpty()) {
            UiObject parent = stack.pop();
            for (int i = 0; i < parent.childCount(); i++) {
                executor.executeLoop(stack, parent.child(i), i);
            }

            if (filter.filter(parent)) {
                result.add(parent);
                if (result.size() >= limit) {
                    break;
                }
            } else {
                if (parent != root) {
                    parent.recycle();
                }
            }
        }
        return result;
    }

    private LoopExecutor createExecutor() {
        // dx不支持函数式 需要写成匿名内部类
        return AlgorithmChanger.enableLogging
                ? new LoopExecutor() {
            @Override
            public void executeLoop(Deque<UiObject> stack, UiObject child, int i) {
                executeLoopWithLog(stack, child, i);
            }
        } : new LoopExecutor() {
            @Override
            public void executeLoop(Deque<UiObject> stack, UiObject child, int i) {
                executeLoopWithoutLog(stack, child, i);
            }
        };
    }


    private interface LoopExecutor {
        void executeLoop(Deque<UiObject> stack, UiObject child, int i);
    }

    private void executeLoopWithLog(Deque<UiObject> stack, UiObject child, int i) {
        if (child != null) {
            if (child.visibleToUser()) {
                Log.d(TAG, "child " + i + "is visible depth:" + child.depth() + "id[" + child.id()+"] bounds:" + child.bounds());
                stack.push(child);
            } else {
                Log.d(TAG, "child not visible drop: id[" + child.id() + "] bounds:" + child.bounds());
                child.recycle();
            }
            stack.push(child);
        }
    }

    private void executeLoopWithoutLog(Deque<UiObject> stack, UiObject child, int i) {
        if (child != null) {
            if (child.visibleToUser()) {
                stack.push(child);
            } else {
                child.recycle();
            }
        }
    }
}
