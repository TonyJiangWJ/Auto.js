package com.tony.autojs.search;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.stardust.autojs.BuildConfig;
import com.stardust.autojs.core.accessibility.AccessibilityBridge;
import com.stardust.automator.UiObject;
import com.stardust.automator.filter.Filter;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class UiObjectTreeBuilder extends ParallelPreBuildTreeSearch {
    private final static String TAG = "UiObjectTreeBuilder";

    private AccessibilityBridge mAccessibilityBridge;

    public UiObjectTreeBuilder(AccessibilityBridge accessibilityBridge) {
        this.mAccessibilityBridge = accessibilityBridge;
    }

    public List<TreeNode> buildTreeNode() {
        List<AccessibilityNodeInfo> roots = mAccessibilityBridge.windowRoots();
        if (BuildConfig.DEBUG)
            Log.d(TAG, "find: roots = " + roots);
        if (roots.isEmpty()) {
            return Collections.emptyList();
        }
        List<TreeNode> result = new ArrayList<>();
        for (AccessibilityNodeInfo root : roots) {
            if (root == null) {
                continue;
            }
            try {
                result.add(buildAllTree(UiObject.Companion.createRoot(root)));
            } catch (Exception e) {
                Log.e(TAG, "buildTreeNode: ", e);
            }
        }
        return result;
    }

    public List<TreeNode> buildVisibleTreeNode() {
        List<AccessibilityNodeInfo> roots = mAccessibilityBridge.windowRoots();
        if (BuildConfig.DEBUG)
            Log.d(TAG, "find: roots = " + roots);
        if (roots.isEmpty()) {
            return Collections.emptyList();
        }
        List<TreeNode> result = new ArrayList<>();
        for (AccessibilityNodeInfo root : roots) {
            if (root == null) {
                continue;
            }
            try {
                result.add(buildTree(UiObject.Companion.createRoot(root), true));
            } catch (Exception e) {
                Log.e(TAG, "buildTreeNode: ", e);
            }
        }
        return result;
    }

    @NonNull
    @Override
    public ArrayList<UiObject> search(@NonNull UiObject root, @NonNull Filter filter, int limit) {
        return new ArrayList<>();
    }
}
