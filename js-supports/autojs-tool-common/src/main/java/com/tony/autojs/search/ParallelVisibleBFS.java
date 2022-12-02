package com.tony.autojs.search;

import android.util.Log;

import com.stardust.automator.UiObject;
import com.stardust.automator.filter.Filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import androidx.annotation.NonNull;

public class ParallelVisibleBFS extends ParallelPreBuildTreeSearch {
    private static final String TAG = "ParallelVisibleBFS";

    @NonNull
    @Override
    public ArrayList<UiObject> search(@NonNull UiObject root, @NonNull Filter filter, int limit) {
        Log.d(TAG, "search limit: " + limit);
        ArrayList<UiObject> result = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        TreeNode rootNode = null;
        Set<UiObject> recycledSet = new HashSet<>();
        try {
            rootNode = buildVisibleTree(root);
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
            if (!isTarget && top.getRoot() != root && top.getChildList().size() == 0) {
                if (!recycledSet.contains(top.getRoot())) {
                    recycledSet.add(top.getRoot());
                    top.getRoot().recycle();
                } else {
                    Log.d(TAG, "already recycled:" + top.getRoot());
                }
            }
        }
        rootNode.recycle();
        return result;
    }

}
