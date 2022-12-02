package com.tony.autojs.search;

import com.stardust.automator.UiObject;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TreeNode {
    private UiObject root;
    private List<TreeNode> childList;

    public TreeNode(UiObject root) {
        this.root = root;
        childList = new CopyOnWriteArrayList<>();
    }

    public void addChild(TreeNode child) {
        childList.add(child);
    }

    public UiObject getRoot() {
        return root;
    }

    public List<TreeNode> getChildList() {
        return childList;
    }

    public void recycle() {
        childList.clear();
    }
}
