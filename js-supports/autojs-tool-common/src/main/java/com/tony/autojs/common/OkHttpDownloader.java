package com.tony.autojs.common;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpDownloader {
    private static final String TAG = "OkHttpDownloader";

    public static void download(final String url, final String filePath) {
        Log.d(TAG, "准备下载url:" + url + " filePath:" + filePath);
        Request request = new Request.Builder().url(url).build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "下载失败 url：" + url + e.getMessage(), e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                byte[] buf = new byte[2048];
                int len = 0;
                ResponseBody responseBody = response.body();
                if (!response.isSuccessful() || responseBody == null) {
                    return;
                }
                File file = new File(filePath);
                try (
                        InputStream is = responseBody.byteStream();
                        FileOutputStream fos = new FileOutputStream(file)
                ) {
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.flush();
                    Log.d(TAG, "下载完成 url:" + url + " filePath:" + filePath);
                } catch (Exception e) {
                    Log.e(TAG, "下载失败 url:" + url + " filePath:" + filePath + e.getMessage(), e);
                    Log.d(TAG, "下载失败 删除文件：" + file.delete());
                }
            }
        });

    }
}
