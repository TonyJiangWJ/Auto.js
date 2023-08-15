package org.autojs.autojs.build;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.android.apksig.ApkSigner;
import com.android.apksigner.PasswordRetriever;
import com.android.apksigner.SignerParams;
import com.stardust.app.GlobalAppContext;
import com.stardust.autojs.apkbuilder.ApkPackager;
import com.stardust.autojs.apkbuilder.ManifestEditor;
import com.stardust.autojs.apkbuilder.util.StreamUtils;
import com.stardust.autojs.project.BuildInfo;
import com.stardust.autojs.project.ProjectConfig;
import com.stardust.autojs.script.EncryptedScriptFileHeader;
import com.stardust.autojs.script.JavaScriptFileSource;
import com.stardust.pio.PFiles;
import com.stardust.util.AdvancedEncryptionStandard;
import com.stardust.util.MD5;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import pxb.android.StringItem;
import pxb.android.axml.AxmlWriter;
import zhao.arsceditor.ArscUtil;
import zhao.arsceditor.ResDecoder.ARSCDecoder;
import zhao.arsceditor.ResDecoder.data.ResTable;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;


/**
 * Created by Stardust on 2017/10/24.
 */

public class ApkBuilder {

    private final static String TAG = "ApkBuilder";

    public interface ProgressCallback {
        void onPrepare(ApkBuilder builder);

        void onBuild(ApkBuilder builder);

        void onSign(ApkBuilder builder);

        void onClean(ApkBuilder builder);

    }

    public static class AppConfig {
        String appName;
        String versionName;
        int versionCode;
        String sourcePath;
        String packageName;
        ArrayList<File> ignoredDirs = new ArrayList<>();
        Callable<Bitmap> icon;
        Boolean useOpenCv = false;
        Boolean usePaddleOcr = false;
        Boolean useMlKitOcr = false;
        Boolean useTessTwo = false;
        Set<String> enabledPermission = new HashSet<>();

        public static AppConfig fromProjectConfig(String projectDir, ProjectConfig projectConfig) {
            String icon = projectConfig.getIcon();
            AppConfig appConfig = new AppConfig()
                    .setAppName(projectConfig.getName())
                    .setPackageName(projectConfig.getPackageName())
                    .ignoreDir(new File(projectDir, projectConfig.getBuildDir()))
                    .setVersionCode(projectConfig.getVersionCode())
                    .setVersionName(projectConfig.getVersionName())
                    .setSourcePath(projectDir);
            if (icon != null) {
                appConfig.setIcon(new File(projectDir, icon).getPath());
            }
            return appConfig;
        }


        public AppConfig ignoreDir(File dir) {
            ignoredDirs.add(dir);
            return this;
        }

        public AppConfig setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public AppConfig setVersionName(String versionName) {
            this.versionName = versionName;
            return this;
        }

        public AppConfig setVersionCode(int versionCode) {
            this.versionCode = versionCode;
            return this;
        }

        public AppConfig setSourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public AppConfig setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }


        public AppConfig setIcon(Callable<Bitmap> icon) {
            this.icon = icon;
            return this;
        }

        public AppConfig setIcon(String iconPath) {
            icon = () -> BitmapFactory.decodeFile(iconPath);
            return this;
        }

        public String getAppName() {
            return appName;
        }

        public String getVersionName() {
            return versionName;
        }

        public int getVersionCode() {
            return versionCode;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public String getPackageName() {
            return packageName;
        }

        public Boolean getUseOpenCv() {
            return useOpenCv;
        }

        public void setUseOpenCv(Boolean useOpenCv) {
            this.useOpenCv = useOpenCv;
        }

        public Boolean getUsePaddleOcr() {
            return usePaddleOcr;
        }

        public void setUsePaddleOcr(Boolean usePaddleOcr) {
            this.usePaddleOcr = usePaddleOcr;
        }

        public Boolean getUseMlKitOcr() {
            return useMlKitOcr;
        }

        public void setUseMlKitOcr(Boolean useMlKitOcr) {
            this.useMlKitOcr = useMlKitOcr;
        }

        public Boolean getUseTessTwo() {
            return useTessTwo;
        }

        public void setUseTessTwo(Boolean useTessTwo) {
            this.useTessTwo = useTessTwo;
        }

        public Set<String> getEnabledPermission() {
            return enabledPermission;
        }

        public void setEnabledPermission(Set<String> enabledPermission) {
            this.enabledPermission = enabledPermission;
        }
    }

    private ProgressCallback mProgressCallback;
    private ApkPackager mApkPackager;
    private String mArscPackageName;
    private ManifestEditor mManifestEditor;
    private String mWorkspacePath;
    private AppConfig mAppConfig;
    private final File mOutApkFile;
    private String mInitVector;
    private String mKey;


    public ApkBuilder(InputStream apkInputStream, File outApkFile, String workspacePath) {
        mWorkspacePath = workspacePath;
        mOutApkFile = outApkFile;
        mApkPackager = new ApkPackager(apkInputStream, mWorkspacePath);
        PFiles.ensureDir(outApkFile.getPath());
    }

    public ApkBuilder setProgressCallback(ProgressCallback callback) {
        mProgressCallback = callback;
        return this;
    }

    public ApkBuilder prepare() throws IOException {
        if (mProgressCallback != null) {
            GlobalAppContext.post(() -> mProgressCallback.onPrepare(ApkBuilder.this));
        }
        (new File(mWorkspacePath)).mkdirs();
        mApkPackager.unzip();
        return this;
    }

    /**
     * 移除未使用的资源文件
     */
    private void removeUnusedAssets() {
        List<String> removeSoList = new ArrayList<>();
        List<String> removeAssetsList = new ArrayList<>();
        if (!mAppConfig.useTessTwo) {
            removeSoList.addAll(Arrays.asList("libtess.so", "liblept.so", "libjpgt.so", "libpngt.so"));
        }
        if (!mAppConfig.useOpenCv) {
            removeSoList.add("libopencv_java4.so");
        }
        if (!mAppConfig.usePaddleOcr) {
            removeAssetsList.add("models");
            removeSoList.add("libNative.so");
            removeSoList.add("libpaddle_light_api_shared.so");
        }
        if (!mAppConfig.useMlKitOcr) {
            removeAssetsList.add("mlkit-google-ocr-models");
            removeSoList.add("libmlkit_google_ocr_pipeline.so");
        }
        List<String> abiList = Arrays.asList("arm64-v8a", "armeabi-v7a", "x86", "x86_64");
        for (String removeSo : removeSoList) {
            for (String abi : abiList) {
                File soFile = new File(mWorkspacePath + "/lib/" + abi + "/" + removeSo);
                if (soFile.exists()) {
                    Log.d(TAG, "removeUnusedAssets: delete lib file:" + soFile.getPath() + " result: " + soFile.delete());
                }
            }
        }
        for (String removeAsset : removeAssetsList) {
            File assetFile = new File(mWorkspacePath + "/assets/" + removeAsset);
            if (assetFile.exists()) {
                Log.d(TAG, "removeUnusedAssets: delete asset file:" + assetFile.getPath() + " result: " + deleteFolder(assetFile));
            }
        }
    }

    public static boolean deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        return folder.delete();
    }

    public ApkBuilder setScriptFile(String path) throws IOException {
        if (PFiles.isDir(path)) {
            copyDir("assets/project/", path);
        } else {
            replaceFile("assets/project/main.js", path);
        }
        return this;
    }

    public void copyDir(String relativePath, String path) throws IOException {
        File fromDir = new File(path);
        File toDir = new File(mWorkspacePath, relativePath);
        toDir.mkdir();
        File[] subFiles = fromDir.listFiles();
        if (subFiles == null) {
            return;
        }
        for (File child : subFiles) {
            if (child.isFile()) {
                if (child.getName().endsWith(".js")) {
                    encrypt(toDir, child);
                } else {
                    StreamUtils.write(new FileInputStream(child),
                            new FileOutputStream(new File(toDir, child.getName())));
                }
            } else {
                if (!mAppConfig.ignoredDirs.contains(child)) {
                    copyDir(PFiles.join(relativePath, child.getName() + "/"), child.getPath());
                }
            }
        }
    }

    private void encrypt(File toDir, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(new File(toDir, file.getName()));
        encrypt(fos, file);
    }

    private void encrypt(FileOutputStream fos, File file) throws IOException {
        try {
            EncryptedScriptFileHeader.INSTANCE.writeHeader(fos, (short) new JavaScriptFileSource(file).getExecutionMode());
            byte[] bytes = new AdvancedEncryptionStandard(mKey.getBytes(), mInitVector).encrypt(PFiles.readBytes(file.getPath()));
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


    public ApkBuilder replaceFile(String relativePath, String newFilePath) throws IOException {
        if (newFilePath.endsWith(".js")) {
            encrypt(new FileOutputStream(new File(mWorkspacePath, relativePath)), new File(newFilePath));
        } else {
            StreamUtils.write(new FileInputStream(newFilePath), new FileOutputStream(new File(mWorkspacePath, relativePath)));
        }
        return this;
    }

    public ApkBuilder withConfig(AppConfig config) throws IOException {
        mAppConfig = config;
        mManifestEditor = editManifest()
                .setAppName(config.appName)
                .setVersionName(config.versionName)
                .setVersionCode(config.versionCode)
                .setPackageName(config.packageName);
        setArscPackageName(config.packageName);
        updateProjectConfig(config);
        setScriptFile(config.sourcePath);
        return this;
    }

    public ManifestEditor editManifest() throws FileNotFoundException {
        mManifestEditor = new ManifestEditorWithAuthorities(new FileInputStream(getManifestFile()));
        return mManifestEditor;
    }

    protected File getManifestFile() {
        return new File(mWorkspacePath, "AndroidManifest.xml");
    }

    private void updateProjectConfig(AppConfig appConfig) {
        ProjectConfig config;
        if (!PFiles.isDir(appConfig.sourcePath)) {
            config = new ProjectConfig()
                    .setMainScriptFile("main.js")
                    .setName(appConfig.appName)
                    .setPackageName(appConfig.packageName)
                    .setVersionName(appConfig.versionName)
                    .setVersionCode(appConfig.versionCode);
            config.setBuildInfo(BuildInfo.generate(appConfig.versionCode));
            PFiles.write(new File(mWorkspacePath, "assets/project/project.json").getPath(), config.toJson());
        } else {
            config = ProjectConfig.fromProjectDir(appConfig.sourcePath);
            long buildNumber = config.getBuildInfo().getBuildNumber();
            config.setBuildInfo(BuildInfo.generate(buildNumber + 1));
            PFiles.write(ProjectConfig.configFileOfDir(appConfig.sourcePath), config.toJson());
        }
        mKey = MD5.md5(config.getPackageName() + config.getVersionName() + config.getMainScriptFile());
        mInitVector = MD5.md5(config.getBuildInfo().getBuildId() + config.getName()).substring(0, 16);
    }

    public ApkBuilder build() throws IOException {
        removeUnusedAssets();
        if (mProgressCallback != null) {
            GlobalAppContext.post(() -> mProgressCallback.onBuild(ApkBuilder.this));
        }
        mManifestEditor.commit();
        if (mAppConfig.icon != null) {
            try {
                Bitmap bitmap = mAppConfig.icon.call();
                if (bitmap != null) {
                    File iconFile = new File(mWorkspacePath, "res/drawable/inrt_launcher.png");
                    if (iconFile.exists()) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100,
                                new FileOutputStream(iconFile));
                    } else {
                        // 新版本打包的图标文件被混淆了，需要解析resources.arsc文件，读取对应的图标文件
                        ArscUtil arscUtil = new ArscUtil();
                        AtomicBoolean replaceSuccess = new AtomicBoolean(false);
                        arscUtil.openArsc(mWorkspacePath + "/resources.arsc", (config, type, key, value) -> {
                            if ("mipmap".equals(type) || "drawable".equals(type)) {
                                if ("inrt_launcher".equals(key) || "ic_launcher".equals(key)) {
                                    Log.d(TAG, String.format("找到了图标文件为：%s %s/%s => %s", config, type, key, value));
                                    File asrcIconFile = new File(mWorkspacePath, value);
                                    try {
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(asrcIconFile));
                                        replaceSuccess.set(true);
                                    } catch (Exception e) {
                                        Log.e(TAG, "build: 替换图标文件异常", e);
                                    }
                                }
                            }
                            return null;
                        });
                        if (!replaceSuccess.get()) {
                            GlobalAppContext.toast("图标文件未找到，替换图标失败");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "build: 替换图标异常", e);
                GlobalAppContext.toast("替换图标异常，生成Apk将不支持自定义图标");
            }
        }
        if (mManifestEditor != null) {
            mManifestEditor.writeTo(new FileOutputStream(getManifestFile()));
        }
        if (mArscPackageName != null) {
            buildArsc();
        }
        return this;
    }

    public ApkBuilder sign() throws Exception {
        if (mProgressCallback != null) {
            GlobalAppContext.post(() -> mProgressCallback.onSign(ApkBuilder.this));
        }
        try (FileOutputStream fos = new FileOutputStream(mOutApkFile)) {
            // 替换TinySign的签名打包操作为基础的zip打包
            ZipApkUtil.packageApk(new File(mWorkspacePath), fos);
        }
        /*
          TODO 进行zipalign，不进行也能够正常安装，暂时不去实现
               目前找到的参考项目为 https://github.com/iyxan23/zipalign-java
               直接引用有问题，等有时间了再去研究
        */
        // 使用ApkSigner进行v2签名
        signWithApkSigner(mOutApkFile);
        return this;
    }

    private void signWithApkSigner(File mOutApkFile) throws IOException {
        copyInputStreamToFile(GlobalAppContext.get().getAssets().open("keystore/buildpkg.bks"), new File(mWorkspacePath + "/buildpkg.bks"));
        File tmpOutputApk = new File(mWorkspacePath + "/temp.apk");
        List<ApkSigner.SignerConfig> signerConfigs = Collections.singletonList(buildSignConfig());
        com.android.apksig.ApkSigner.Builder apkSignerBuilder = (new com.android.apksig.ApkSigner.Builder(signerConfigs))
                .setInputApk(mOutApkFile).setOutputApk(tmpOutputApk)
                .setOtherSignersSignaturesPreserved(false)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(false);

        ApkSigner apkSigner = apkSignerBuilder.build();

        try {
            apkSigner.sign();
        } catch (Exception e) {
            Log.e("ApkBuilder", "signWithApkSigner failed: ", e);
            throw new RuntimeException(e);
        }
        try {
            copyFile(tmpOutputApk, mOutApkFile);
        } catch (Exception e) {
            Log.e("ApkBuilder", "signWithApkSigner: 覆盖签名APK异常", e);
            throw new RuntimeException(e);
        }
    }

    private ApkSigner.SignerConfig buildSignConfig() {
        PasswordRetriever passwordRetriever = new PasswordRetriever();
        SignerParams signer = new SignerParams();
        signer.setKeystoreFile(mWorkspacePath + "/buildpkg.bks");
        Log.d(TAG, "buildSignConfig: store file " + mWorkspacePath + "/buildpkg.bks");
        signer.setName("signer #1");
        signer.setKeystorePasswordSpec("pass:buildpkg");
        signer.setKeystoreKeyAlias("buildpkg");
        try {
            signer.loadPrivateKeyAndCerts(passwordRetriever);
        } catch (Exception e) {
            Log.e(TAG, "buildSignConfig: Failed to load signer \"" + signer.getName() + "\": " + e.getMessage(), e);
            throw new RuntimeException("Failed to load signer " + e.getMessage());
        }

        String v1SigBasename;
        if (signer.getV1SigFileBasename() != null) {
            v1SigBasename = signer.getV1SigFileBasename();
        } else if (signer.getKeystoreKeyAlias() != null) {
            v1SigBasename = signer.getKeystoreKeyAlias();
        } else if (signer.getKeyFile() != null) {
            String keyFileName = new File(signer.getKeyFile()).getName();
            int delimiterIndex = keyFileName.indexOf('.');
            if (delimiterIndex == -1) {
                v1SigBasename = keyFileName;
            } else {
                v1SigBasename = keyFileName.substring(0, delimiterIndex);
            }
        } else {
            throw new RuntimeException("Neither KeyStore key alias nor private key file available");
        }
        Log.d(TAG, "buildSignConfig: sign base name is " + v1SigBasename);
        return (new ApkSigner.SignerConfig.Builder(v1SigBasename, signer.getPrivateKey(), signer.getCerts())).build();
    }

    public ApkBuilder cleanWorkspace() {
        if (mProgressCallback != null) {
            GlobalAppContext.post(() -> mProgressCallback.onClean(ApkBuilder.this));
        }
        delete(new File(mWorkspacePath));
        return this;
    }

    public ApkBuilder setArscPackageName(String packageName) throws IOException {
        mArscPackageName = packageName;
        return this;
    }

    private void buildArsc() throws IOException {
        File oldArsc = new File(mWorkspacePath, "resources.arsc");
        File newArsc = new File(mWorkspacePath, "resources.arsc.new");
        ARSCDecoder decoder = new ARSCDecoder(new BufferedInputStream(new FileInputStream(oldArsc)), (ResTable) null, false);
        FileOutputStream fos = new FileOutputStream(newArsc);
        decoder.CloneArsc(fos, mArscPackageName, true);
        oldArsc.delete();
        newArsc.renameTo(oldArsc);
    }

    private void delete(File file) {
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();

            for (File child : files) {
                delete(child);
            }
            file.delete();
        }
    }

    private class ManifestEditorWithAuthorities extends ManifestEditor {

        ManifestEditorWithAuthorities(InputStream manifestInputStream) {
            super(manifestInputStream);
        }

        @Override
        public void onAttr(AxmlWriter.Attr attr) {
            if ("authorities".equals(attr.name.data) && attr.value instanceof StringItem) {
                ((StringItem) attr.value).data = mAppConfig.packageName + ".fileprovider";
            } else {
                super.onAttr(attr);
            }
        }

        @Override
        public boolean filterPermission(String permissionName) {
            Log.d(TAG, "filterPermission: " + permissionName);
            boolean persist = mAppConfig.getEnabledPermission().contains(permissionName);
            return !persist;
        }
    }
}
