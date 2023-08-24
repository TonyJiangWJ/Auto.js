package com.stardust.autojs.apkbuilder;

import android.text.TextUtils;

import com.stardust.autojs.apkbuilder.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 负责将内置apk解压到指定目录
 *
 * Created by Stardust on 2017/10/23.
 */
public class ApkUnpackUtil {

    private final InputStream mApkInputStream;
    private final String mWorkspacePath;

    public ApkUnpackUtil(InputStream apkInputStream, String workspacePath) {
        mApkInputStream = apkInputStream;
        mWorkspacePath = workspacePath;
    }

    public void unzip() throws IOException {
        try (ZipInputStream zis = new ZipInputStream(mApkInputStream)) {
            for (ZipEntry e = zis.getNextEntry(); e != null; e = zis.getNextEntry()) {
                String name = e.getName();
                if (!e.isDirectory() && !TextUtils.isEmpty(name)) {
                    File file = new File(mWorkspacePath, name);
                    System.out.println(file);
                    File parentFile = file.getParentFile();
                    if (parentFile != null && (parentFile.exists() || parentFile.mkdirs())) {
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            System.out.println(file.getName() + " has been written");
                            StreamUtils.write(zis, fos);
                        }
                    } else {
                        System.out.println(file.getName() + " can not write");
                        System.out.println("is parent null? " + (parentFile != null));
                        if (file.getParentFile() != null) {
                            System.out.println("is parent exists? " + parentFile.exists());
                            System.out.println("can parent mkdirs? " + parentFile.mkdirs());
                        }
                    }
                } else {
                    System.out.println("file or empty：" + name);
                }
            }
        }
    }
}
