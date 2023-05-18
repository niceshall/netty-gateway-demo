package com.example.nettygatewaydemo.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @description: properties配置工具类
 */
@Component
public class PropertiesConfigUtils implements EnvironmentAware, ApplicationContextAware {
    private static ApplicationContext localApplicationContext;
    private static Environment environment = new StandardEnvironment();
    private static PropertiesConfigUtils instance = new PropertiesConfigUtils();

    public static PropertiesConfigUtils getInstance() {
        return instance;
    }

    public static String getProperty(String placeholder) {
        String result = null;
        ApplicationContext applicationContext = getInstance().getApplicationContext();
        if (applicationContext != null && applicationContext.getParent() != null) {
            result = applicationContext.getParent().getEnvironment().getProperty(placeholder);
        }

        if (result == null && getInstance().environment != null) {
            result = getInstance().environment.getProperty(placeholder);
        }

        return result;
    }

    public static String getProperty(String placeholder, String defaultValue) {
        String result = getProperty(placeholder);
        if (!StringUtils.hasText(result)) {
            result = defaultValue;
        }
        return result;
    }

    public ApplicationContext getApplicationContext() {
        return this.localApplicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.localApplicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
