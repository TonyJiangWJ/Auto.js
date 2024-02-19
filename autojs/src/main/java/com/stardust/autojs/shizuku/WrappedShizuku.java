package com.stardust.autojs.shizuku;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.stardust.app.GlobalAppContext;
import com.stardust.autojs.annotation.ScriptInterface;
import com.stardust.autojs.runtime.api.AbstractShell;
import com.tony.shizuku.shell.plugin.IUserService;
import com.stardust.autojs.BuildConfig;

import rikka.shizuku.Shizuku;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * shizuku 封装
 * 参考自:
 *  SuperMonster003/Auto.js (https://github.com/SuperMonster003/AutoJs6)
 *  RikkaApps/Shizuku-API (https://github.com/RikkaApps/Shizuku-API)
 */
public class WrappedShizuku {
    private final String TAG = "WrappedShizuku";
    private final int BIND_USER_SERVICE = "request-user-service".hashCode();

    private static volatile WrappedShizuku INSTANCE;

    private IUserService userService;
    private boolean hasBinder = false;
    private final boolean isInstalled;

    private WrappedShizuku() {
        isInstalled = getLaunchIntent() != null;
    }

    public static WrappedShizuku getInstance() {
        if (INSTANCE == null) {
            synchronized (WrappedShizuku.class) {
                if (INSTANCE == null) {
                    INSTANCE = new WrappedShizuku();
                }
            }
        }
        return INSTANCE;
    }

    private final Shizuku.OnBinderReceivedListener BINDER_RECEIVED_LISTENER = () -> {
        if (Shizuku.isPreV11()) {
            Log.d(TAG, "Shizuku pre-v11 is not supported");
        } else {
            Log.d(TAG, "Binder received");
            hasBinder = true;
        }
    };
    private final Shizuku.OnBinderDeadListener BINDER_DEAD_LISTENER = () -> {
        Log.d(TAG, "Binder dead");
        hasBinder = false;
    };
    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        if (grantResult == PERMISSION_GRANTED && requestCode == BIND_USER_SERVICE) {
            bindUserService();
        } else {
            Log.d(TAG, "User denied permission");
        }
    }

    private final ServiceConnection userServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            StringBuilder res = new StringBuilder();
            res.append("onServiceConnected: ").append(componentName.getClassName()).append('\n');

            if (binder != null && binder.pingBinder()) {
                userService = IUserService.Stub.asInterface(binder);
                res.append("success");
                try {
                    Log.d(TAG, "onServiceConnected: " + userService.execCommand("test"));
                } catch (RemoteException e) {
                    Log.e(TAG, "onServiceConnected fail: ", e);
                }
            } else {
                res.append("invalid binder for ").append(componentName).append(" received");
            }
            Log.d(TAG, "onServiceConnected: " + (res.toString().trim()));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: " + '\n' + componentName.getClassName());
        }
    };

    private Shizuku.UserServiceArgs userServiceArgs;

    private void bindUserService() {
        StringBuilder res = new StringBuilder();
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10");
            } else {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection);
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            res.append(tr);
        }
        Log.d(TAG, "bindUserService: " + (res.toString().trim()));
    }

    public void onCreate(String applicationId, int versionCode) {
        userServiceArgs = new Shizuku.UserServiceArgs(new ComponentName(applicationId, UserService.class.getName()))
                .daemon(false)
                .processNameSuffix("service")
                .debuggable(BuildConfig.DEBUG)
                .version(versionCode);
        Shizuku.addBinderReceivedListenerSticky(BINDER_RECEIVED_LISTENER);
        Shizuku.addBinderDeadListener(BINDER_DEAD_LISTENER);
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    }

    public void requestPermission() {
        if (!isInstalled) {
            return;
        }
        if (!isShizukuRunning()) {
            return;
        }
        if (isRunning()) {
            return;
        }
        Shizuku.requestPermission(BIND_USER_SERVICE);
    }

    public Intent getLaunchIntent() {
        Intent intent = GlobalAppContext.get().getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    public String exec(String cmd) throws RemoteException {
        ensureService();
        return userService.execCommand(cmd);
    }

    private void ensureService() {
        if (userService == null) {
            throw new IllegalStateException("未绑定shizuku服务");
        }
    }

    @ScriptInterface
    public AbstractShell.Result execCommand(String cmd) {
        ensureService();
        try {
            return AbstractShell.Result.fromJson(userService.execCommand(cmd));
        } catch (Throwable e) {
            AbstractShell.Result result = new AbstractShell.Result();
            result.code = 1;
            String error = e.getMessage();
            if (error == null) {
                if (!hasPermission()) {
                    error = "No permission to access Shizuku";
                } else if (!isShizukuRunning()) {
                    error = "Shizuku service may be not running";
                }
            }
            result.error = error;
            result.result = "";
            Log.e(TAG, "execCommand failed", e);
            return result;
        }
    }

    public boolean hasPermission() {
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isShizukuRunning() {
        return hasBinder;
    }

    public boolean isRunning() {
        return userService != null;
    }

    public boolean isInstalled() {
        return isInstalled;
    }

    @ScriptInterface
    public AbstractShell.Result execCommand(String[] cmdList) {
        return execCommand(joinToString(cmdList, "\n"));
    }

    private String joinToString(String[] cmdList, String s) {
        StringBuilder sb = new StringBuilder();
        for (String cmd : cmdList) {
            sb.append(cmd).append(s);
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }


}
