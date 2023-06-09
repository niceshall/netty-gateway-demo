package com.example.nettygatewaydemo.core.request;


import com.example.nettygatewaydemo.model.RouteDefine;
import io.netty.handler.codec.http.HttpRequest;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * <p>
 * 此时Netty作为客户端转发HTTP请求，最简单的方法就是使用Netty提供的HttpRequest，略加封装
 * </p>
 * <p>
 * 一些开源项目选择自己定义HttpRequest和HttpResponse
 * </p>
 *
 * @create: 2022/5/19
 */
public class NettyClientHttpRequest {

    public static final String HTTP = "HTTP";

    public static final String HTTPS = "HTTPS";

    private RouteDefine route;
    private URI uri;
    private HttpRequest httpRequest;

    public NettyClientHttpRequest(RouteDefine route, URI uri, HttpRequest httpRequest) {
        this.route = route;
        this.uri = uri;
        this.httpRequest = httpRequest;
    }

    /**
     * 获取转发请求的Host
     *
     * @return
     */
    public String getHost() {
        if (uri.getHost() == null) {
            throw new RuntimeException("no host found");
        }
        return uri.getHost();
    }

    /**
     * 5
     * 获取端口号
     *
     * @return
     */
    public int getPort() {
        URL url = null;
        try {
            url = uri.toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        String protocol = url.getProtocol() == null ? HTTP : url.getProtocol();
        int port = url.getPort();
        if (port == -1) {
            if (HTTP.equalsIgnoreCase(protocol)) {
                port = 80;
            } else if (HTTPS.equalsIgnoreCase(protocol)) {
                port = 443;
            }
        }
        return port;
    }


    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(getHost(), getPort());
    }

    public HttpRequest getHttpRequest() {
        return this.httpRequest;
    }

    @Override
    public String toString() {
        return "NettyClientHttpRequest{" +
                "uri=" + uri +
                '}';
    }
}
