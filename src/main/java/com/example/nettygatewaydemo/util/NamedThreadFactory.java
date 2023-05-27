package com.example.nettygatewaydemo.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);
    private final AtomicInteger mThreadNum = new AtomicInteger(1);
    private final String mPrefix;
    private final boolean mDaemo;
    private final ThreadGroup mGroup;

    public NamedThreadFactory() {
        this("janus-" + POOL_SEQ.getAndIncrement() + "-thread-");
    }

    public NamedThreadFactory(String prefix) {
        this(prefix, false);
    }

    public NamedThreadFactory(String prefix, boolean daemo) {
        if (prefix != null) {
            this.mPrefix = prefix + "-thread-";
        } else {
            this.mPrefix = ("janus-" + POOL_SEQ.getAndIncrement() + "-thread-");
        }
        this.mDaemo = daemo;
        SecurityManager s = System.getSecurityManager();
        this.mGroup = (s == null ? Thread.currentThread().getThreadGroup() : s.getThreadGroup());
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = this.mPrefix + this.mThreadNum.getAndIncrement();
        Thread thread = new Thread(this.mGroup, runnable, name, 0L);
        thread.setDaemon(this.mDaemo);
        return thread;
    }
}
