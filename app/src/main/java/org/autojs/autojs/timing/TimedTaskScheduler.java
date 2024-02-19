package org.autojs.autojs.timing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.autojs.autojs.Pref;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.external.ScriptIntents;
import org.autojs.autojs.timing.work.AlarmManagerProvider;
import org.autojs.autojs.timing.work.AndroidJobProvider;
import org.autojs.autojs.timing.work.WorkManagerProvider;
import org.autojs.autojs.timing.work.WorkProvider;
import org.autojs.autojs.timing.work.WorkProviderConstants;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by Stardust on 2017/11/27.
 */

public class TimedTaskScheduler {

    private static final String LOG_TAG = "TimedTaskScheduler";
    private static final long SCHEDULE_TASK_MIN_TIME = TimeUnit.DAYS.toMillis(2);

    protected static final String JOB_TAG_CHECK_TASKS = "checkTasks";

    @SuppressLint("CheckResult")
    public void checkTasks(Context context, boolean force) {
        autoJsLog("check tasks: force = " + force);
        TimedTaskManager.getInstance().getAllTasks()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(timedTask -> scheduleTaskIfNeeded(context, timedTask, force));
    }

    public void scheduleTaskIfNeeded(Context context, TimedTask timedTask, boolean force) {
        long millis = timedTask.getNextTime();
        if (millis <= System.currentTimeMillis()) {
            autoJsLog("task out date, just run it: " + timedTask);
            runTask(context, timedTask);
            return;
        }
        if ((!force && timedTask.isScheduled()) || millis - System.currentTimeMillis() > SCHEDULE_TASK_MIN_TIME) {
            return;
        }
        scheduleTask(context, timedTask, millis, force);
        TimedTaskManager.getInstance().notifyTaskScheduled(timedTask);
    }

    /**
     * only available in WorkManagerProvider and AndroidJobProvider
     *
     * @param context
     * @param timedTask
     * @param millis
     * @param force
     */
    public synchronized void scheduleTask(Context context, TimedTask timedTask, long millis, boolean force) {
        if (!force && timedTask.isScheduled()) {
            return;
        }
        long timeWindow = millis - System.currentTimeMillis();
        timedTask.setScheduled(true);
        TimedTaskManager.getInstance().updateTaskWithoutReScheduling(timedTask);

        autoJsLog("schedule task: task = " + timedTask + ", millis = " + millis + ", timeWindow = " + timeWindow);

        getWorkProvider(context).enqueueWork(timedTask, timeWindow);
    }

    public static void cancel(TimedTask timedTask, Context context) {
        getWorkProvider(context).cancel(timedTask);
    }

    public static void init(@NotNull Context context) {
        createCheckWorker(context, 20);
        getWorkProvider(context).checkTasks(context, true);
    }

    private static void createCheckWorker(Context context, int delay) {
        autoJsLog("创建定期检测任务");
        getWorkProvider(context).enqueuePeriodicWork(delay);
    }

    protected static void runTask(Context context, TimedTask task) {
        autoJsLog("run task: task = " + task);
        Intent intent = task.createIntent();
        ScriptIntents.handleIntent(context, intent);
        TimedTaskManager.getInstance().notifyTaskFinished(task.getId());
        // 如果队列中有任务正在等待，直接取消
        getWorkProvider(context).cancel(task);
    }

    public static synchronized void ensureCheckTaskWorks(Context context) {
        try {
            boolean workFine = getWorkProvider(context).isCheckWorkFine();
            // 校验是否有超时未执行的
            final long currentMillis = System.currentTimeMillis();
            boolean anyLost = TimedTaskManager.getInstance().getAllTasks().any(task -> {
                if (task.getNextTime() < currentMillis) {
                    autoJsLog("task timeout: " + task.toString() + " nextTime:" + task.getNextTime() + " current millis:" + currentMillis);
                    return true;
                } else {
                    return false;
                }
            }).blockingGet();
            if (!workFine || anyLost) {
                autoJsLog("ensureCheckTaskWorks: " + (workFine ? "PeriodicWork works fine, but missed some work" : "PeriodicWork died"));
                createCheckWorker(context, 0);
                getWorkProvider(context).checkTasks(context, true);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "获取定时校验任务失败");
        }
    }

    public static WorkProvider getWorkProvider(Context context) {
        String currentActive = Pref.getCurrentManager();
        if (WorkProviderConstants.WORK_MANAGER_PROVIDER.equals(currentActive)) {
            Log.d(LOG_TAG, "当前启用的定时任务方式为WorkManager");
            return WorkManagerProvider.getInstance(context);
        } else if (WorkProviderConstants.ANDROID_JOB_PROVIDER.equals(currentActive)) {
            Log.d(LOG_TAG, "当前启用的定时任务方式为AndroidJob");
            return AndroidJobProvider.getInstance(context);
        } else {
            Log.d(LOG_TAG, "当前启用的定时任务方式为AlarmManager");
            return AlarmManagerProvider.getInstance(context);
        }
    }

    private static void autoJsLog(String content) {
        Log.d(LOG_TAG, content);
        AutoJs.getInstance().debugInfo(content);
    }
}
