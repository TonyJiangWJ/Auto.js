package org.autojs.autojs

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.multidex.MultiDexApplication
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.core.ui.inflater.ImageLoader
import com.stardust.autojs.core.ui.inflater.util.Drawables
import com.stardust.autojs.runtime.api.Device
import com.stardust.theme.ThemeColor
import com.tencent.bugly.Bugly
import com.tencent.bugly.crashreport.CrashReport
import org.apache.commons.lang3.StringUtils
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.autojs.key.GlobalKeyObserver
import org.autojs.autojs.external.receiver.DynamicBroadcastReceivers
import org.autojs.autojs.external.receiver.MediaButtonReceiver
import org.autojs.autojs.theme.ThemeColorManagerCompat
import org.autojs.autojs.timing.TimedTaskManager
import org.autojs.autojs.timing.TimedTaskScheduler
import org.autojs.autojs.tool.CrashHandler
import org.autojs.autojs.ui.error.ErrorReportActivity
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by Stardust on 2017/1/27.
 */

class App : MultiDexApplication() {
    lateinit var dynamicBroadcastReceivers: DynamicBroadcastReceivers
        private set

    private var buttonReceiver: MediaButtonReceiver? = null

    override fun onCreate() {
        super.onCreate()
        GlobalAppContext.set(this)
        instance = WeakReference(this)
        setUpStaticsTool()
        setUpDebugEnvironment()
        init()
    }

    private fun setUpStaticsTool() {
        if (BuildConfig.DEBUG || StringUtils.isEmpty(BuildConfig.POSTHOG_APP_ID))
            return
        // Create a PostHog Config with the given API key and host
        val config = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_APP_ID,
            host = "https://app.posthog.com"
        )

        // Setup PostHog with the given Context and Config
        PostHogAndroid.setup(this, config)
    }

    @SuppressLint("HardwareIds")
    private fun setUpDebugEnvironment() {
        if (StringUtils.isEmpty(BuildConfig.BUGLY_APP_ID)) {
            return
        }
        Bugly.isDev = BuildConfig.DEBUG
        val crashHandler = CrashHandler(ErrorReportActivity::class.java)

        val strategy = CrashReport.UserStrategy(applicationContext)
        strategy.setCrashHandleCallback(crashHandler)
        strategy.deviceModel = Device.model
        strategy.deviceID =
            Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        CrashReport.setIsDevelopmentDevice(this, BuildConfig.DEBUG)

        CrashReport.initCrashReport(applicationContext, BuildConfig.BUGLY_APP_ID, false, strategy)

        crashHandler.setBuglyHandler(Thread.getDefaultUncaughtExceptionHandler())
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)

    }

    private fun init() {
        ThemeColorManagerCompat.init(
            this,
            ThemeColor(
                resources.getColor(R.color.colorPrimary),
                resources.getColor(R.color.colorPrimaryDark),
                resources.getColor(R.color.colorAccent)
            )
        )
        AutoJs.initInstance(this)
        if (Pref.isRunningVolumeControlEnabled()) {
            GlobalKeyObserver.init()
        }
        setupDrawableImageLoader()
        TimedTaskScheduler.init(this)
        initDynamicBroadcastReceivers()
    }

    @SuppressLint("CheckResult")
    private fun initDynamicBroadcastReceivers() {
        dynamicBroadcastReceivers = DynamicBroadcastReceivers(this)
        val localActions = ArrayList<String>()
        val actions = ArrayList<String>()
        TimedTaskManager.getInstance().allIntentTasks
            .filter { task -> task.action != null }
            .doOnComplete {
                if (localActions.isNotEmpty()) {
                    dynamicBroadcastReceivers.register(localActions, true)
                }
                if (actions.isNotEmpty()) {
                    dynamicBroadcastReceivers.register(actions, false)
                }
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
                    Intent(
                        DynamicBroadcastReceivers.ACTION_STARTUP
                    )
                )
            }
            .subscribe({
                if (it.isLocal) {
                    localActions.add(it.action)
                } else {
                    actions.add(it.action)
                }
            }, { it.printStackTrace() })
        ensureKeyCodeReceiver(false)
    }
    fun ensureKeyCodeReceiver(forceEnable: Boolean) {
        if (Pref.isHyperOSKeyCode() || forceEnable) {
            if (buttonReceiver == null) {
                val filter = IntentFilter()
                filter.addAction(MediaButtonReceiver.ACTION)
                buttonReceiver = MediaButtonReceiver()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(buttonReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    registerReceiver(buttonReceiver, filter);
                }
            }
        }
    }

    fun unregisterButtonReceiver() {
        if (buttonReceiver != null) {
            unregisterReceiver(buttonReceiver)
            buttonReceiver = null
        }
    }

    private fun setupDrawableImageLoader() {
        Drawables.setDefaultImageLoader(object : ImageLoader {
            override fun loadInto(imageView: ImageView, uri: Uri) {
                Glide.with(imageView)
                    .load(uri)
                    .into(imageView)
            }

            override fun loadIntoBackground(view: View, uri: Uri) {
                Glide.with(view)
                    .load(uri)
                    .into(object : SimpleTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            view.background = resource
                        }
                    })
            }

            override fun load(view: View, uri: Uri): Drawable {
                throw UnsupportedOperationException()
            }

            override fun load(
                view: View,
                uri: Uri,
                drawableCallback: ImageLoader.DrawableCallback
            ) {
                Glide.with(view)
                    .load(uri)
                    .into(object : SimpleTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            drawableCallback.onLoaded(resource)
                        }
                    })
            }

            override fun load(view: View, uri: Uri, bitmapCallback: ImageLoader.BitmapCallback) {
                Glide.with(view)
                    .asBitmap()
                    .load(uri)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            bitmapCallback.onLoaded(resource)
                        }
                    })
            }
        })
    }

    companion object {

        private val TAG = "App"

        private lateinit var instance: WeakReference<App>

        val app: App
            get() = instance.get()!!
    }


}
