package com.tony.autojs.search;

import java.util.concurrent.*;

/**
 * 自动关闭的线程池，当无任务提交的idleTimeout(timeUnit)之后，检查当前线程池是否空闲，如果是则关闭线程池
 * @author TonyJiangWJ
 * @since 2025/1/24
 */
public class AutoCloseThreadPool {
    private final ThreadPoolExecutor threadPool;
    private final ScheduledExecutorService monitor;
    private final long idleTimeout;
    private final TimeUnit timeUnit;
    private ScheduledFuture<?> shutdownTask;
    private final Runnable callback;

    public AutoCloseThreadPool(int corePoolSize, int maxPoolSize, long idleTimeout, TimeUnit timeUnit,
                               ThreadFactory threadFactory,
                               Runnable callback) {
        this.threadPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS, // 非核心线程空闲存活时间
                new LinkedBlockingQueue<Runnable>(),
                threadFactory
        );
        this.monitor = Executors.newSingleThreadScheduledExecutor();
        this.idleTimeout = idleTimeout;
        this.timeUnit = timeUnit;
        this.callback = callback;
    }

    public void execute(Runnable task) {
        synchronized (this) {
            // 提交任务前检查线程池是否已关闭
            if (threadPool.isShutdown()) {
                throw new RejectedExecutionException("ThreadPool已关闭，无法接受新任务");
            }
            // 取消之前的关闭任务
            if (shutdownTask != null && !shutdownTask.isDone()) {
                shutdownTask.cancel(false);
            }
            // 提交任务
            threadPool.execute(task);
            // 调度新的关闭检查任务
            scheduleShutdownCheck();
        }
    }

    private void scheduleShutdownCheck() {
        shutdownTask = monitor.schedule(new Runnable() {
            @Override
            public void run() {
                synchronized (AutoCloseThreadPool.this) {
                    // 检查活动线程数和任务队列
                    if (threadPool.getActiveCount() == 0 && threadPool.getQueue().isEmpty()) {
                        // 关闭线程池和监控
                        threadPool.shutdown();
                        monitor.shutdown();
                        // 执行线程池关闭回调
                        if (callback != null) {
                            callback.run();
                        }
                    }
                }
            }
        }, idleTimeout, timeUnit);
    }

    public void shutdownNow() {
        synchronized (this) {
            threadPool.shutdownNow();
            monitor.shutdownNow();
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return threadPool.awaitTermination(timeout, unit);
    }
}
