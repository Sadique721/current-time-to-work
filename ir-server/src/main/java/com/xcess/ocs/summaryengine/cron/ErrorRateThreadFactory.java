package com.xcess.ocs.summaryengine.cron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ErrorRateThreadFactory implements ThreadFactory {

    private static final Logger logger = LoggerFactory.getLogger(ErrorRateThreadFactory.class);
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public ErrorRateThreadFactory(String requestId) {
        namePrefix = requestId;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
        if (t.isDaemon()) {
            t.setDaemon(true);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        t.setContextClassLoader(getClass().getClassLoader());
        return t;
    }
}
