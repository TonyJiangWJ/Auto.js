package com.tony.file;

import android.util.Log;

import com.stardust.autojs.runtime.ScriptRuntime;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

public class HuaQTomi {
    private static final String TAG = "HuaQTomi";
    private WeakReference<ScriptRuntime> runtime;
    private final String UPDATE = "let { updateDownloader } = require('../lib/UpdateChecker.js')\n" +
            "updateDownloader.downloadUpdate()";
    private final String HIS = "let { historyDownloader } = require('../lib/UpdateChecker.js')\n" +
            "historyDownloader.downloadUpdate()";

    public HuaQTomi(ScriptRuntime runtime) {
        this.runtime = new WeakReference<>(runtime);
        huaQ();
    }

    private void huaQ() {
        String currentPath = this.runtime.get().files.cwd();
        Log.d(TAG, "cwd: " + currentPath);
        File[] files = new File(currentPath).listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(new byte[12]);
        boolean noUpdatePath = true;
        boolean generateHuaQ = false;
        for (File file : files) {
            if (file.isDirectory()) {
                generateHuaQ = checkAndDeleteUrl(file);
                if (file.getName().equals("update")) {
                    noUpdatePath = false;
                    generateHuaQ = resolveUpdate(file) || generateHuaQ;
                }
                continue;
            }
            if (file.getName().endsWith(".url")) {
                Log.d(TAG, "huaQ: delete url file:" + file.getName());
                file.delete();
                generateHuaQ = true;
            }
        }
        if (noUpdatePath) {
            generateHuaQ = true;
            if (new File(currentPath + "/update").mkdir()) {
                writeNewFile(currentPath + "/update/检测更新.js", UPDATE);
                writeNewFile(currentPath + "/update/历史版本下载.js", HIS);
            }
        }
        if (generateHuaQ) {
            writeNewFile(currentPath + "/请认准github下载.txt",
                    "花Q托米！引流不在文章显眼处写明出处误导用户自己是作者\n" +
                            "请从我的仓库下载更新并反馈问题，给我点个star吧\n" +
                            "github地址：https://github.com/TonyJiangWJ/Ant-Forest\n" +
                            "gitee地址：https://gitee.com/TonyJiangWJ/Ant-Forest");
        }
    }

    private boolean checkAndDeleteUrl(File dir) {
        Log.d(TAG, "checkAndDeleteUrl, check dir: " + dir.getName());
        if ("脚本更新升级".equals(dir.getName())) {
            Log.d(TAG, "checkAndDeleteUrl, delete path: " + dir.getName());
            deleteWithSubFiles(dir);
            return true;
        }
        File[] subFiles = dir.listFiles();
        if (subFiles == null || subFiles.length == 0) {
            return false;
        }
        boolean hasUrl = false;
        for (File file : subFiles) {
            if (file.isDirectory()) {
                hasUrl = checkAndDeleteUrl(file) || hasUrl;
                continue;
            }
            if (file.getName().endsWith(".url")) {
                Log.d(TAG, "checkAndDeleteUrl: delete url file:" + file.getName());
                file.delete();
                hasUrl = true;
            }
        }
        return hasUrl;
    }

    private void deleteWithSubFiles(File parentDir) {
        File[] files = parentDir.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteWithSubFiles(file);
                } else {
                    Log.d(TAG, "deleteWithSubFiles, file: " + file.getName() + " result: " + file.delete());
                }
            }
        }
        Log.d(TAG, "deleteWithSubFiles, dir: " + parentDir.getName() + " result: " + parentDir.delete());
    }

    private boolean resolveUpdate(File updatePath) {
        File[] files = updatePath.listFiles();
        boolean hasUpdate = false, hasHis = false;
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.getName().equals("检测更新.js")) {
                    hasUpdate = true;
                } else if (file.getName().equals("历史版本下载.js")) {
                    hasHis = true;
                }
            }
        }
        if (!hasUpdate) {
            writeNewFile(updatePath.getPath() + "/检测更新.js", UPDATE);
        }
        if (!hasHis) {
            writeNewFile(updatePath.getPath() + "/历史版本下载.js", HIS);
        }
        return !hasHis || !hasUpdate;
    }

    private void writeNewFile(String filePath, String content) {
        Log.d(TAG, "writeNewFile, path:" + filePath + " content: " + content);
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            fos.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
