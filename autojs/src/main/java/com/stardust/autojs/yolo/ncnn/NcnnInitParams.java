package com.stardust.autojs.yolo.ncnn;

import com.stardust.autojs.yolo.ModelInitParams;

public class NcnnInitParams extends ModelInitParams {
    private String paramPath;
    private String binPath;

    private boolean useGpu;

    public String getParamPath() {
        return paramPath;
    }

    public void setParamPath(String paramPath) {
        this.paramPath = paramPath;
    }

    public String getBinPath() {
        return binPath;
    }

    public void setBinPath(String binPath) {
        this.binPath = binPath;
    }

    public boolean isUseGpu() {
        return useGpu;
    }

    public void setUseGpu(boolean useGpu) {
        this.useGpu = useGpu;
    }
}
