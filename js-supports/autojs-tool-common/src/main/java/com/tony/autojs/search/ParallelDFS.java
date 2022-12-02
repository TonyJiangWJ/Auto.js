package com.tony.autojs.search;

import android.util.Log;

import com.stardust.automator.UiObject;
import com.stardust.automator.filter.Filter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

import androidx.annotation.NonNull;

public class ParallelDFS extends ParallelPreBuildTreeSearch {
    private static final String TAG = "ParallelVisibleDFS";

    @NonNull
    @Override
    public ArrayList<UiObject> search(@NonNull UiObject root, @NonNull Filter filter, int limit) {
        Log.d(TAG, "search limit: " + limit);
        ArrayList<UiObject> result = new ArrayList<>();
        Deque<TreeNode> stack = new LinkedList<>();
        TreeNode rootNode;
        try {
            rootNode = buildAllTree(root);
            stack.push(rootNode);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>(1);
        }
        while (!stack.isEmpty()) {
            TreeNode parent = stack.pop();
            for (TreeNode child : parent.getChildList()) {
                stack.push(child);
            }

            if (filter.filter(parent.getRoot())) {
                result.add(parent.getRoot());
                if (result.size() >= limit) {
                    break;
                }
            } else {
                if (parent.getRoot() != root) {
                    parent.getRoot().recycle();
                }
            }
        }
        rootNode.recycle();
        return result;
    }

}
