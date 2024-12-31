package org.autojs.autojs.external.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import org.autojs.autojs.autojs.key.GlobalKeyObserver;

public class MediaButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaButtonReceiver";

    public static final String ACTION = "miui.intent.action.KEYCODE_EXTERNAL";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getAction());
        if (ACTION.equals(intent.getAction())) {
            Log.d(TAG, "handle miui.intent.action.KEYCODE_EXTERNAL");
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event != null) {
                Log.d(TAG, "Key pressed: " + event.getKeyCode());
                GlobalKeyObserver.getSingleton().onKeyEvent(event.getKeyCode(), event, true);

            }
        }
    }
}