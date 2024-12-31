package com.stardust.autojs.runtime.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import com.stardust.view.accessibility.OnKeyListener;

public class ButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaButtonReceiver";

    public static final String ACTION = "miui.intent.action.KEYCODE_EXTERNAL";

    private OnKeyListener listener;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getAction());
        if (ACTION.equals(intent.getAction())) {
            Log.d(TAG, "handle miui.intent.action.KEYCODE_EXTERNAL");
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event != null && listener != null) {
                Log.d(TAG, "Key pressed: " + event.getKeyCode());
                listener.onKeyEvent(event.getKeyCode(), event);
            }
        }
    }

    public void setListener(OnKeyListener listener) {
        this.listener = listener;
    }
}
