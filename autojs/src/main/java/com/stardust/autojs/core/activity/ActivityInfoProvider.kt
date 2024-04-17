package com.stardust.autojs.core.activity

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.RequiresApi
import com.stardust.app.isOpPermissionGranted
import com.stardust.autojs.core.util.Shell
import com.stardust.autojs.engine.ScriptEngineManager
import com.stardust.view.accessibility.AccessibilityDelegate
import com.stardust.view.accessibility.AccessibilityService.Companion.instance
import java.util.WeakHashMap
import java.util.regex.Pattern

/**
 * Created by Stardust on 2017/3/9.
 */

class ActivityInfoProvider(private val context: Context, private val scriptEngineManager: ScriptEngineManager) : AccessibilityDelegate {

    private val mPackageManager: PackageManager = context.packageManager

    @Volatile
    private var mLatestPackage: String = ""
    @Volatile
    private var mLatestActivity: String = ""
    private var mLatestComponentFromShell: ComponentName? = null

    private var mShell: Shell? = null
    private var mUseShell = false

    private val checkedPackage: Set<String> = HashSet()
    private val existsPackage: Set<String> = HashSet()

    private val windowIdActivityMap: WeakHashMap<Int, String> = WeakHashMap()

    val latestPackage: String
        get() {
            val compFromShell = mLatestComponentFromShell
            if (useShell && compFromShell != null) {
                return compFromShell.packageName
            }
            if (useUsageStats && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                mLatestPackage = getLatestPackageByUsageStats()
            }
            return mLatestPackage
        }

    val latestActivity: String
        get() {
            val compFromShell = mLatestComponentFromShell
            if (useShell && compFromShell != null) {
                return compFromShell.className
            }
            return mLatestActivity
        }

    var useUsageStats: Boolean = false

    var useShell: Boolean
        get() = mUseShell
        set(value) {
            if (value) {
                mShell.let {
                    if (it == null) {
                        mShell = createShell(200)
                    }
                }
            } else {
                mShell?.exit()
                mShell = null
            }
            mUseShell = value
        }

    override val eventTypes: Set<Int>?
        get() = AccessibilityDelegate.ALL_EVENT_TYPES

    override fun onAccessibilityEvent(service: AccessibilityService, event: AccessibilityEvent): Boolean {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.className != null) {
                windowIdActivityMap[event.windowId] = event.className as String
            }
            if (scriptEngineManager.engines.size == 0) {
                return false
            }
            val start: Long = System.currentTimeMillis()
            val window = service.getWindow(event.windowId)
            Log.d(LOG_TAG, "get window cost: " + (System.currentTimeMillis() - start) + "ms")
            if (window?.isFocused != false) {
                setLatestComponent(event.packageName, event.className, false)
                return false
            }
        }
        return false
    }

    fun getPackageAndActivityInfoByA11y() {
        val start: Long = System.currentTimeMillis()
        instance?.windows?.forEach { window ->
            run {
                if (window?.isFocused != false && window?.root != null) {
                    setLatestComponent(window.root.packageName, windowIdActivityMap[window.id], true)
                }
            }
        }
        Log.d(LOG_TAG, "get window cost: " + (System.currentTimeMillis() - start) + "ms")
    }

    fun getLatestPackageByUsageStatsIfGranted(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && context.isOpPermissionGranted(AppOpsManager.OPSTR_GET_USAGE_STATS)) {
            return getLatestPackageByUsageStats()
        }
        return mLatestPackage
    }

    private fun setLatestComponentFromShellOutput(output: String) {
        val matcher = WINDOW_PATTERN.matcher(output)
        if (!matcher.find() || matcher.groupCount() < 1) {
            Log.w(LOG_TAG, "invalid format: $output")
            return
        }
        val latestPackage = matcher.group(1)
        if (latestPackage == null || latestPackage.contains(":")) {
            return
        }
        val latestActivity = if (matcher.groupCount() >= 2) {
            matcher.group(2).orEmpty()
        } else {
            ""
        }
        Log.d(LOG_TAG, "setLatestComponent: output = $output, comp = $latestPackage/$latestActivity")
        mLatestComponentFromShell = ComponentName(latestPackage, latestActivity)
    }

    private fun createShell(dumpInterval: Int): Shell {
        val shell = Shell(true)
        shell.setCallback(object : Shell.Callback {
            override fun onOutput(str: String) {

            }

            override fun onNewLine(line: String) {
                setLatestComponentFromShellOutput(line)
            }

            override fun onInitialized() {
            }

            override fun onInterrupted(e: InterruptedException) {

            }
        })
        shell.exec(DUMP_WINDOW_COMMAND.format(dumpInterval))
        return shell
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun getLatestPackageByUsageStats(): String {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val current = System.currentTimeMillis()
        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, current - 60 * 60 * 1000, current)
        return if (usageStats.isEmpty()) {
            mLatestPackage
        } else {
            usageStats.sortBy {
                it.lastTimeStamp
            }
            usageStats.last().packageName
        }

    }

    private fun setLatestComponent(latestPackage: CharSequence?, latestClass: CharSequence?, notCoverA6y: Boolean) {
        if (latestPackage == null)
            return
        val latestPackageStr = latestPackage.toString()
        val latestClassStr = (latestClass ?: "").toString()
        if (isPackageExists(latestPackageStr)) {
            mLatestPackage = latestPackage.toString()
            if (latestClassStr != "" || !notCoverA6y) {
                mLatestActivity = latestClassStr
            }
        }
        Log.d(LOG_TAG, "setLatestComponent: $latestPackage/$latestClassStr $mLatestPackage/$mLatestActivity")
    }

    private fun isPackageExists(packageName: String): Boolean {
        if (checkedPackage.contains(packageName)) {
            return existsPackage.contains(packageName)
        }
        checkedPackage.plus(packageName)
        return try {
            mPackageManager.getPackageInfo(packageName, 0)
            existsPackage.plus(packageName)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        private val WINDOW_PATTERN = Pattern.compile("Window\\{\\S+\\s\\S+\\s([^\\/]+)\\/?([^}]+)?\\}")
        private val DUMP_WINDOW_COMMAND = """
            oldActivity=""
            currentActivity=`dumpsys window windows | grep -E 'mCurrentFocus'`
            while true
            do
                if [[ ${'$'}oldActivity != ${'$'}currentActivity && ${'$'}currentActivity != *"=null"* ]]; then
                    echo ${'$'}currentActivity
                    oldActivity=${'$'}currentActivity
                fi
                currentActivity=`dumpsys window windows | grep -E 'mCurrentFocus'`
            done
        """.trimIndent()

        private const val LOG_TAG = "ActivityInfoProvider"
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun AccessibilityService.getWindow(windowId: Int): AccessibilityWindowInfo? {
    windows.forEach {
        if (it.id == windowId) {
            return it
        }
    }
    return null
}
