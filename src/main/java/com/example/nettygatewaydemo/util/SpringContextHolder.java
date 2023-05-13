package com.example.nettygatewaydemo.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @Description
 *
 * @Auther niceshall
 * @Date 2019/2/25 14:31
 * @Version 1.0
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext context = null;

    public static ApplicationContext getContext() {
        return context;
    }

    public static synchronized void setContext(ApplicationContext context) {
        SpringContextHolder.context = context;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.setContext(applicationContext);
    }
}
