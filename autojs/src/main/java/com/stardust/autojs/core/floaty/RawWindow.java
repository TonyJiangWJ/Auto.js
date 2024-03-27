package com.stardust.autojs.core.floaty;

import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;

import com.stardust.autojs.R;
import com.stardust.autojs.core.ui.ViewExtras;
import com.stardust.autojs.core.ui.inflater.inflaters.Exceptions;
import com.stardust.autojs.runtime.exception.ScriptInterruptedException;
import com.stardust.concurrent.VolatileDispose;
import com.stardust.enhancedfloaty.FloatyService;
import com.stardust.enhancedfloaty.FloatyWindow;
import com.stardust.enhancedfloaty.util.WindowTypeCompat;
import com.litiaotiao.app.AccessibilityService;

public class RawWindow extends FloatyWindow {



    public interface RawFloaty {

        View inflateWindowView(FloatyService service, ViewGroup parent);
    }

    private VolatileDispose<RuntimeException> mInflateException = new VolatileDispose<>();
    private RawFloaty mRawFloaty;
    private View mContentView;

    public RawWindow(RawFloaty rawFloaty) {
        mRawFloaty = rawFloaty;
    }

    @Override
    public void onCreate(FloatyService floatyService, WindowManager windowManager) {
        try {
            super.onCreate(floatyService, windowManager);
        } catch (RuntimeException e) {
            mInflateException.setAndNotify(e);
            return;
        }
        mInflateException.setAndNotify(Exceptions.NO_EXCEPTION);
    }

    @Override
    protected View onCreateView(FloatyService floatyService) {
        ViewGroup windowView = (ViewGroup) View.inflate(floatyService, R.layout.raw_window, null);
        mContentView = mRawFloaty.inflateWindowView(floatyService, windowView);
        return windowView;
    }

    public RuntimeException waitForCreation() {
        return mInflateException.blockedGetOrThrow(ScriptInterruptedException.class);
    }

    public View getContentView() {
        return mContentView;
    }

    @Override
    protected WindowManager.LayoutParams onCreateWindowLayoutParams() {
        int flags =
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        }
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowType(),
                flags,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.START;

        return layoutParams;
    }

    private int getWindowType() {
        if (AccessibilityService.Companion.getInstance()!= null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        }
        return WindowTypeCompat.getWindowType();
    }

    public void disableWindowFocus() {
        WindowManager.LayoutParams windowLayoutParams = getWindowLayoutParams();
        windowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        updateWindowLayoutParams(windowLayoutParams);
    }

    public void requestWindowFocus() {
        WindowManager.LayoutParams windowLayoutParams = getWindowLayoutParams();
        windowLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        updateWindowLayoutParams(windowLayoutParams);
        getWindowView().requestLayout();
    }

    public void setTouchable(boolean touchable) {
        WindowManager.LayoutParams windowLayoutParams = getWindowLayoutParams();
        if (touchable) {
            windowLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        } else {
            windowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        updateWindowLayoutParams(windowLayoutParams);
    }

    @Override
    public void close() {
        super.close();
        if (mContentView == null) {
            return;
        }
        ViewExtras.recycle(mContentView);
        ViewParent parent = mContentView.getParent();
        if (parent instanceof View) {
            ViewExtras.recycle((View)parent);
        }
        mContentView = null;
    }
}
