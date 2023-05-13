package com.example.nettygatewaydemo.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

/**
 * @author lw
 */
public class RequestUtils {

    public static FullHttpRequest create(FullHttpRequest fullHttpRequest, ByteBuf content) {
        return new DefaultFullHttpRequest(
                fullHttpRequest.protocolVersion(), fullHttpRequest.method(), fullHttpRequest.uri(), content);
    }

    public static FullHttpRequest create(String method, String uri, String version) {
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        HttpVersion httpVersion = HttpVersion.valueOf(version);
        return new DefaultFullHttpRequest(httpVersion, httpMethod, uri, Unpooled.EMPTY_BUFFER);
    }
}
