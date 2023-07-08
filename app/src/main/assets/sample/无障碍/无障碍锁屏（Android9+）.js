auto.waitFor()
if (!automator.lockScreen()) {
    toastLog('仅安卓9以上支持无障碍锁屏')
}