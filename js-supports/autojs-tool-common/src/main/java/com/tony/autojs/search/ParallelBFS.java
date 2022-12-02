package com.tony.autojs.search;

import android.util.Log;

import com.stardust.automator.UiObject;
import com.stardust.automator.filter.Filter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import androidx.annotation.NonNull;

/**
 *
 */
public class ParallelBFS extends ParallelPreBuildTreeSearch {
    private static final String TAG = "ParallelVisibleBFS";

    @NonNull
    @Override
    public ArrayList<UiObject> search(@NonNull UiObject root, @NonNull Filter filter, int limit) {
        Log.d(TAG, "search limit: " + limit);
        ArrayList<UiObject> result = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        TreeNode rootNode = null;
        try {
            rootNode = buildAllTree(root);
            queue.add(rootNode);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>(1);
        }
        while (!queue.isEmpty()) {
            TreeNode top = queue.poll();
            if (top == null) {
                continue;
            }
            boolean isTarget = filter.filter(top.getRoot());
            if (isTarget) {
                result.add(top.getRoot());
                if (result.size() >= limit) {
                    return result;
                }
            }
            queue.addAll(rootNode.getChildList());
            if (!isTarget && top.getRoot() != root) {
                top.getRoot().recycle();
            }
        }
        rootNode.recycle();
        return result;
    }

}
