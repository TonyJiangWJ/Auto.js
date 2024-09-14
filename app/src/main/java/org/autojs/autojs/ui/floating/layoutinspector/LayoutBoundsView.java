package org.autojs.autojs.ui.floating.layoutinspector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.stardust.view.accessibility.NodeInfo;
import com.stardust.util.ViewUtil;

import org.autojs.autojs.BuildConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Stardust on 2017/3/10.
 */

public class LayoutBoundsView extends View {
    private static final String TAG = "LayoutBoundsView";

    private static final int COLOR_SHADOW = 0x6a000000;
    private NodeInfo mRootNode;
    private NodeInfo mTouchedNode;
    private Paint mBoundsPaint;
    private Paint mFillingPaint;
    private OnNodeInfoSelectListener mOnNodeInfoSelectListener;
    private int mTouchedNodeBoundsColor = Color.RED;
    private int mNormalNodeBoundsColor = Color.GREEN;
    private Rect mTouchedNodeBounds;

    private int[] mBoundsInScreen;
    protected int mStatusBarHeight;

    public LayoutBoundsView(Context context) {
        super(context);
        init();
    }

    public LayoutBoundsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LayoutBoundsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LayoutBoundsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    public void setOnNodeInfoSelectListener(OnNodeInfoSelectListener onNodeInfoSelectListener) {
        mOnNodeInfoSelectListener = onNodeInfoSelectListener;
    }

    public void setRootNode(NodeInfo rootNode) {
        Log.d(TAG, "setRootNode: ");
        displayNodeLayout(rootNode, 0);
        mRootNode = rootNode;
        mTouchedNode = null;
    }

    private void displayNodeLayout(NodeInfo rootNode, int depth) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        if (rootNode == null) {
            return;
        }
        if (!rootNode.getHidden()) {
            Log.d(TAG, String.format("%s%s", String.join("", Collections.nCopies(depth, "-")), rootNode));
        }
        for (NodeInfo child : rootNode.getChildren()) {
            displayNodeLayout(child, depth + 1);
        }
    }


    private void init() {
        mBoundsPaint = new Paint();
        mBoundsPaint.setStyle(Paint.Style.STROKE);
        mFillingPaint = new Paint();
        mFillingPaint.setStyle(Paint.Style.FILL);
        mFillingPaint.setColor(COLOR_SHADOW);
        setWillNotDraw(false);
        mStatusBarHeight = ViewUtil.getStatusBarHeight(getContext());
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBoundsInScreen == null) {
            mBoundsInScreen = new int[4];
            getLocationOnScreen(mBoundsInScreen);
            mStatusBarHeight = mBoundsInScreen[1];
        }
        if (mTouchedNode != null) {
            canvas.save();
            if (mTouchedNodeBounds == null) {
                mTouchedNodeBounds = new Rect(mTouchedNode.getBoundsInScreen());
                mTouchedNodeBounds.offset(0, -mStatusBarHeight);
            }
            canvas.clipRect(mTouchedNodeBounds, Region.Op.DIFFERENCE);
        }
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mFillingPaint);
        if (mTouchedNode != null) {
            canvas.restore();
        }
        mBoundsPaint.setColor(mNormalNodeBoundsColor);
        draw(canvas, mRootNode);
        if (mTouchedNode != null) {
            mBoundsPaint.setColor(mTouchedNodeBoundsColor);
            drawRect(canvas, mTouchedNode.getBoundsInScreen(), mStatusBarHeight, mBoundsPaint);
        }
    }

    public Paint getBoundsPaint() {
        return mBoundsPaint;
    }

    public Paint getFillingPaint() {
        return mFillingPaint;
    }

    public void setTouchedNodeBoundsColor(int touchedNodeBoundsColor) {
        mTouchedNodeBoundsColor = touchedNodeBoundsColor;
    }

    public void setNormalNodeBoundsColor(int normalNodeBoundsColor) {
        mNormalNodeBoundsColor = normalNodeBoundsColor;
    }

    private void draw(Canvas canvas, NodeInfo node) {
        if (node == null)
            return;
        if (!node.getHidden()) {
            drawRect(canvas, node.getBoundsInScreen(), mStatusBarHeight, mBoundsPaint);
        }
        for (NodeInfo child : node.getChildren()) {
            draw(canvas, child);
        }
    }

    static void drawRect(Canvas canvas, Rect rect, int statusBarHeight, Paint paint) {
        Rect offsetRect = new Rect(rect);
        offsetRect.offset(0, -statusBarHeight);
        canvas.drawRect(offsetRect, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, String.format("onTouchEvent: %d, %d", (int) event.getRawX(), (int) event.getRawY()));
        if (mRootNode != null) {
            setSelectedNode(findNodeAt(mRootNode, (int) event.getRawX(), (int) event.getRawY()));
        }
        if (event.getAction() == MotionEvent.ACTION_UP && mTouchedNode != null) {
            onNodeInfoClick(mTouchedNode);
            return true;
        }
        return super.onTouchEvent(event);

    }

    private void onNodeInfoClick(NodeInfo nodeInfo) {
        if (mOnNodeInfoSelectListener != null) {
            mOnNodeInfoSelectListener.onNodeSelect(nodeInfo);
        }
    }

    private NodeInfo findNodeAt(NodeInfo node, int x, int y) {
        ArrayList<NodeInfo> list = new ArrayList<>();
        displayNodeLayout(node, 0);
        findNodeAt(node, x, y, list);
        if (list.isEmpty()) {
            Log.d(TAG, "findNode is empty");
            return null;
        }
        Log.d(TAG, "findNodeAt: list size:" + list.size());
        return Collections.min(list, (o1, o2) ->
                o1.getBoundsInScreen().width() * o1.getBoundsInScreen().height() -
                        o2.getBoundsInScreen().width() * o2.getBoundsInScreen().height());
    }


    private void findNodeAt(NodeInfo node, int x, int y, List<NodeInfo> list) {
        if (node == null) {
            return;
        }
        if (BuildConfig.DEBUG && !node.getHidden()) {
            Log.d(TAG, String.format("%s child: %s", String.join("", Collections.nCopies(node.getDepth(), "-")), node));
        }
        if (node.getBoundsInScreen().contains(x, y) && !node.getHidden()) {
            list.add(node);
        }
        for (NodeInfo child : node.getChildren()) {
            findNodeAt(child, x, y, list);
        }
    }

    public void setSelectedNode(NodeInfo selectedNode) {
        mTouchedNode = selectedNode;
        mTouchedNodeBounds = null;
        invalidate();
    }

    public int getStatusBarHeight() {
        return mStatusBarHeight;
    }

    public void hideAllBoundsSameNode(NodeInfo targetNode) {
        if (mRootNode == null || targetNode == null) {
            return;
        }
        hideNodes(mRootNode, targetNode);
    }

    private void hideNodes(NodeInfo parent, NodeInfo targetNode) {
        if (parent == null) {
            return;
        }
        if (parent.getBoundsInScreen().equals(targetNode.getBoundsInScreen())) {
            parent.setHidden(true);
        }
        if (parent.getChildren().size() == 0) {
            return;
        }
        for (NodeInfo child : parent.getChildren()) {
            hideNodes(child, targetNode);
        }
    }
}
