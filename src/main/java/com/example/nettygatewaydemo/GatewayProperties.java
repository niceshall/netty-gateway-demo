package com.example.nettygatewaydemo;

import com.example.nettygatewaydemo.model.RouteDefine;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @description: 网关配置类
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
@Component
@ConfigurationProperties(GatewayProperties.PREFIX)
public class GatewayProperties {

    public static final String PREFIX = "netty.gateway";

    private List<RouteDefine> routes;

    public List<RouteDefine> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteDefine> routes) {
        this.routes = routes;
    }


}
