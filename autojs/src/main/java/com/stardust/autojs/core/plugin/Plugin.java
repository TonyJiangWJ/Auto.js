package com.stardust.autojs.core.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import com.stardust.autojs.rhino.TopLevelScope;
import com.stardust.autojs.runtime.ScriptRuntime;

import java.lang.reflect.Method;

public class Plugin implements ServiceConnection {
    private static final String TAG = "Plugin";

    public static class PluginLoadException extends RuntimeException {
        public PluginLoadException(Throwable cause) {
            super(cause);
        }

        public PluginLoadException(String message) {
            super(message);
        }
    }

    private static final String KEY_REGISTRY = "org.autojs.plugin.sdk.registry";

    public static Plugin load(Context context, Context packageContext, ScriptRuntime runtime, TopLevelScope scope) {
        try {
            ApplicationInfo applicationInfo = packageContext.getPackageManager().getApplicationInfo(packageContext.getPackageName(), PackageManager.GET_META_DATA);
            String registryClass = applicationInfo.metaData.getString(KEY_REGISTRY);
            if (registryClass == null) {
                throw new PluginLoadException("no registry in metadata");
            }
            Class<?> pluginClass = Class.forName(registryClass, true, packageContext.getClassLoader());
            Method loadDefault = pluginClass.getMethod("loadDefault", Context.class, Context.class, Object.class, Object.class);
            return Plugin.create(loadDefault.invoke(null, context, packageContext, runtime, scope));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        } catch (Throwable e) {
            Log.e(TAG, "load failed", e);
            throw new PluginLoadException(e);
        }
    }

    private static Plugin create(Object pluginInstance) {
        if (pluginInstance == null)
            return null;
        return new Plugin(pluginInstance);
    }

    private Object mPluginInstance;
    private Method mGetVersion;
    private Method mGetScriptDir;
    private String mMainScriptPath;
    // AIDL 调用
    private Method mGetService;
    private Method mOnServiceConnected;
    private Method mOnServiceDisconnected;
    private ComponentName componentName;

    private int version;

    public Plugin(Object pluginInstance) {
        mPluginInstance = pluginInstance;
        findMethods(pluginInstance.getClass());
    }

    @SuppressWarnings("unchecked")
    private void findMethods(Class pluginClass) {
        mGetVersion = findMethod(pluginClass, "getVersion");
        mGetScriptDir = findMethod(pluginClass, "getAssetsScriptDir");
        mGetService = findMethod(pluginClass, "getService");
        mOnServiceConnected = findMethod(pluginClass, "onServiceConnected", ComponentName.class, IBinder.class);
        mOnServiceDisconnected = findMethod(pluginClass, "onServiceDisconnected", ComponentName.class);
        if (mGetService != null) {
            try {
                componentName = (ComponentName) mGetService.invoke(mPluginInstance);
            } catch (Exception e) {
                Log.e(TAG, "get componentName failed ", e);
            }
        }
        try {
            if (mGetVersion != null) {
                version = (int) mGetVersion.invoke(mPluginInstance);
            }
        } catch (Exception e) {
            Log.e(TAG, "get version failed ", e);
        }
    }



    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: " + componentName.getShortClassName());
        if (mOnServiceConnected == null) {
            return;
        }
        try {
            mOnServiceConnected.invoke(mPluginInstance, componentName, iBinder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected: " + componentName.getShortClassName());
        if (mOnServiceDisconnected == null) {
            return;
        }
        try {
            mOnServiceDisconnected.invoke(mPluginInstance, componentName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        mPluginInstance = null;
    }

    private Method findMethod(Class pluginClass, String method, Class ...args) {
        try {
            return pluginClass.getMethod(method, args);
        } catch (NoSuchMethodException e) {
            Log.d(TAG, "can not findMethod: "+ method, e);
        }
        return null;
    }

    public Object unwrap() {
        return mPluginInstance;
    }

    public String getMainScriptPath() {
        return mMainScriptPath;
    }

    public void setMainScriptPath(String mainScriptPath) {
        mMainScriptPath = mainScriptPath;
    }

    public String getAssetsScriptDir() {
        try {
            return (String) mGetScriptDir.invoke(mPluginInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ComponentName getComponentName() {
        return componentName;
    }

    public int getVersion() {
        return version;
    }
}
