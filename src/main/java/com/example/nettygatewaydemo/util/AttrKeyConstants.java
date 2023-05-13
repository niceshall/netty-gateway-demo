package com.example.nettygatewaydemo.util;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.AttributeKey;

public class AttrKeyConstants {

    public static final AttributeKey<WebSocketServerHandshaker> HANDSHAKER_ATTR_KEY =
            AttributeKey.valueOf(WebSocketServerHandshaker.class, "HANDSHAKER");

    public static final AttributeKey<WebSocketClientHandshaker> CLIENT_HANDSHAKER_ATTR_KEY =
            AttributeKey.valueOf(WebSocketClientHandshaker.class, "CLIENT_HANDSHAKER");

    public static final AttributeKey<Channel> CLIENT_CHANNEL =
            AttributeKey.valueOf(Channel.class, "CLIENT_CHANNEL");

    public static final AttributeKey<Boolean> CLIENT_CHANNEL_KEEP_ALIVE =
            AttributeKey.valueOf(Boolean.class, "CLIENT_CHANNEL_KEEP_ALIVE");

}
