package com.tony.autojs.search;

import android.util.Log;

import com.stardust.automator.UiObject;
import com.stardust.automator.search.SearchAlgorithm;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ParallelPreBuildTreeSearch implements SearchAlgorithm {
    private final String TAG = this.getClass().getName();

    public TreeNode buildVisibleTree(UiObject root) throws InterruptedException {
        return buildTree(root, true);
    }

    public TreeNode buildAllTree(UiObject root) throws InterruptedException {
        return buildTree(root, false);
    }

    public TreeNode buildTree(UiObject root, boolean visible) throws InterruptedException {
        long start = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger(0);
        TreeNode rootNode = visible ? buildVisibleTreeNode(root, counter) : buildAllNodeTree(root, counter);
        Log.d(TAG, "search: create node tree cost: " + (System.currentTimeMillis() - start) + "ms " +
                "total nodes: " + counter.get());
        return rootNode;
    }

    private TreeNode buildVisibleTreeNode(final UiObject node, final AtomicInteger counter) throws InterruptedException {
        if (node == null) {
            return null;
        }
        if (!node.visibleToUser()) {
            node.recycle();
            return null;
        }
        final TreeNode root = new TreeNode(node);
        final Deque<TreeNode> stack = new ConcurrentLinkedDeque<>();
        stack.push(root);


        Log.d(TAG, "buildVisibleTreeNode: 开始构建可见控件树");
        while (!stack.isEmpty()) {
            final TreeNode parent = stack.pop();
            final CountDownLatch countDownLatch = new CountDownLatch(parent.getRoot().childCount());
            for (int i = 0; i < parent.getRoot().childCount(); i++) {
                final int finalI = i;
                AlgorithmChanger.threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        UiObject child = parent.getRoot().child(finalI);
                        if (child == null || !child.visibleToUser()) {
                            countDownLatch.countDown();
                            return;
                        }
                        TreeNode childNode = new TreeNode(child);
                        counter.incrementAndGet();
                        parent.addChild(childNode);
                        stack.push(childNode);
                        countDownLatch.countDown();
                    }
                });

            }
            countDownLatch.await();
        }
        Log.d(TAG, "buildVisibleTreeNode: 构建可见控件树完成");
        return root;
    }

    private TreeNode buildAllNodeTree(final UiObject node, final AtomicInteger counter) throws InterruptedException {
        if (node == null) {
            return null;
        }
        Log.d(TAG, "buildAllNodeTree: 开始构建全部控件树");
        final TreeNode root = new TreeNode(node);
        final Deque<TreeNode> stack = new ConcurrentLinkedDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            final TreeNode parent = stack.pop();
            final CountDownLatch countDownLatch = new CountDownLatch(parent.getRoot().childCount());
            for (int i = 0; i < parent.getRoot().childCount(); i++) {
                final int finalI = i;
                AlgorithmChanger.threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        UiObject child = parent.getRoot().child(finalI);
                        if (child == null) {
                            countDownLatch.countDown();
                            return;
                        }
                        TreeNode childNode = new TreeNode(child);
                        counter.incrementAndGet();
                        parent.addChild(childNode);
                        stack.push(childNode);
                        countDownLatch.countDown();
                    }
                });
            }
            countDownLatch.await();
        }
        Log.d(TAG, "buildAllNodeTree: 构建全部控件树完成");
        return root;
    }
}
