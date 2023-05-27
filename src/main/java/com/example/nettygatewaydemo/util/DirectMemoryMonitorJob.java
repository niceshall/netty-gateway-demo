package com.example.nettygatewaydemo.util;

import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import sun.management.ManagementFactoryHelper;

import java.lang.management.BufferPoolMXBean;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class DirectMemoryMonitorJob implements Runnable {
    private static final int _1K = 1024;
    private static final Logger logger = LoggerFactory.getLogger(DirectMemoryMonitorJob.class);
    private AtomicLong directMemory;

    @Override
    public void run() {
        try {
            boolean result = PlatformDependent.useDirectBufferNoCleaner();
            logger.info("netty direct memory type no cleaner: {}", result);
            if (result) {
                directMemory = (AtomicLong) ReflectionUtils.getFieldValue(PlatformDependent.class, "DIRECT_MEMORY_COUNTER");
                if (directMemory != null) {
                    long memoryInKb = directMemory.get() / _1K;
                    logger.info("netty direct memory noCleaner: MemoryUsed {}k", memoryInKb);
                }
            } else {
                /*List<BufferPoolMXBean> bufferPoolMXBeans = ManagementFactoryHelper.getBufferPoolMXBeans();
                BufferPoolMXBean directBufferMXBean = bufferPoolMXBeans.get(0);
                // hasCleaner的DirectBuffer的数量
                long count = directBufferMXBean.getCount();
                // hasCleaner的DirectBuffer的堆外内存占用大小，单位字节
                long memoryUsed = directBufferMXBean.getMemoryUsed();
                long memoryInKb = memoryUsed / _1K;
                logger.info("netty direct memory hasCleaner: DirectBuffer count {}, MemoryUsed {}k", count, memoryInKb);*/
            }
        } catch (Exception e) {
            logger.error("netty direct memory monitor job exception", e);
        }
    }
}
