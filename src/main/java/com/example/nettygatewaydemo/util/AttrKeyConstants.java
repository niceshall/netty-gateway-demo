package com.example.nettygatewaydemo.util;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.AttributeKey;

/**
 * @description: channel属性key常量
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class AttrKeyConstants {

    public static final AttributeKey<WebSocketServerHandshaker> HANDSHAKER_ATTR_KEY =
            AttributeKey.valueOf(WebSocketServerHandshaker.class, "HANDSHAKER");

    // 服务器端与后端服务的websocket握手
    public static final AttributeKey<WebSocketClientHandshaker> CLIENT_HANDSHAKER_ATTR_KEY =
            AttributeKey.valueOf(WebSocketClientHandshaker.class, "CLIENT_HANDSHAKER");

    // 服务器端与后端服务连接生成的channel
    public static final AttributeKey<Channel> CLIENT_CHANNEL =
            AttributeKey.valueOf(Channel.class, "CLIENT_CHANNEL");

    // 服务器端与后端服务是否支持transfer-encoding:chunked
    public static final AttributeKey<Boolean> CLIENT_CHANNEL_TRANSFER_ENCODING_CHUNKED =
            AttributeKey.valueOf(Boolean.class, "CLIENT_CHANNEL_TRANSFER_ENCODING_CHUNKED");

}
