package com.stardust.autojs.runtime.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.stardust.autojs.core.plugin.Plugin;
import com.stardust.autojs.runtime.ScriptRuntime;
import com.stardust.pio.PFiles;

import java.io.File;
import java.lang.ref.WeakReference;

public class Plugins {

    private final Context mContext;
    private final WeakReference<ScriptRuntime> mRuntime;
    private File mPluginCacheDir;

    public Plugins(Context context, ScriptRuntime runtime) {
        mContext = context;
        mRuntime = new WeakReference<>(runtime);
        mPluginCacheDir = new File(mContext.getCacheDir(), "plugin-scripts/");
    }

    public Plugin load(String packageName) {
        try {
            Context packageContext = mContext.createPackageContext(packageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            Plugin plugin = Plugin.load(mContext, packageContext, mRuntime.get(), mRuntime.get().getTopLevelScope());
            if (plugin == null) {
                return null;
            }
            File scriptCacheDir = getScriptCacheDir(packageName);
            PFiles.copyAssetDir(packageContext.getAssets(), plugin.getAssetsScriptDir(), scriptCacheDir.getPath(), null);
            plugin.setMainScriptPath(new File(scriptCacheDir, "index.js").getPath());
            bindService(plugin);
            return plugin;
        } catch (Exception e) {
            throw new Plugin.PluginLoadException(e);
        }
    }

    private void bindService(Plugin plugin) {
        if (plugin.getVersion() < 2) {
            return;
        }
        ComponentName componentName = plugin.getComponentName();
        if (componentName == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setComponent(componentName);
        try {
            mContext.getApplicationContext().bindService(intent, plugin, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e("Plugins", "bindService failed", e);
        }
    }

    private File getScriptCacheDir(String packageName) {
        File dir = new File(mPluginCacheDir, packageName + "/");
        dir.mkdirs();
        return dir;
    }

    public void clear() {
        PFiles.deleteRecursively(mPluginCacheDir);
    }

}
