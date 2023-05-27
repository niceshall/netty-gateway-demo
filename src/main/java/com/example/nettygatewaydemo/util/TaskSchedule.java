package com.example.nettygatewaydemo.util;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TaskSchedule implements ApplicationRunner, Ordered {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DirectMemoryMonitor", true))
                .scheduleAtFixedRate(new DirectMemoryMonitorJob(), 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
