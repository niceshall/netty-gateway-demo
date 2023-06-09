package com.example.nettygatewaydemo.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import static com.example.nettygatewaydemo.util.AttrKeyConstants.CLIENT_HANDSHAKER_ATTR_KEY;
import static com.example.nettygatewaydemo.util.AttrKeyConstants.HANDSHAKER_ATTR_KEY;

/**
 * @description: websocket工具类
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class WsUtils {

    private final static Logger logger = LoggerFactory.getLogger(WsUtils.class);

    /**
     * 服务端与客户端握手
     *
     * @param ctx
     * @param request
     * @return
     * @throws URISyntaxException
     */
    public static boolean wsServerHandshakeWithClient(ChannelHandlerContext ctx, HttpRequest request, String remoteWsUri, Channel outboundChannel) throws URISyntaxException {
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                request.uri(), null, false);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            // connect to remote ws server
            ChannelFuture handshake1 = handshaker.handshake(ctx.channel(), request);
            handshake1.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        logger.info("server handshake with remote server success");
                    } else {
                        logger.info("server handshake with remote server fail");
                    }
                }
            });
            ctx.channel().attr(HANDSHAKER_ATTR_KEY).set(handshaker);


            // handshake with remote ws server
            remoteWsUri = remoteWsUri.replaceFirst("http", "ws");
            proxyWsClientHandshake(remoteWsUri, outboundChannel);
            return true;
        }
        return false;
    }

    /**
     * 与远程ws服务握手
     */
    public static void proxyWsClientHandshake(String remoteWsUri, Channel outboundChannel) throws URISyntaxException {
        URI uri = new URI(remoteWsUri);
        WebSocketClientHandshaker clientHandshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders());
        if (clientHandshaker == null) {
            HttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UPGRADE_REQUIRED, outboundChannel.alloc().buffer(0));
            res.headers().set(HttpHeaderNames.SEC_WEBSOCKET_VERSION, WebSocketVersion.V13.toHttpHeaderValue());
            HttpUtil.setContentLength(res, 0L);
            outboundChannel.writeAndFlush(res, outboundChannel.newPromise());
        } else {
            // connect to remote ws server
            ChannelFuture handshake = clientHandshaker.handshake(outboundChannel);
            handshake.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        logger.info("client handshake with remote server success");
                    } else {
                        logger.info("client handshake with remote server fail");
                    }
                }
            });
            outboundChannel.attr(CLIENT_HANDSHAKER_ATTR_KEY).set(clientHandshaker);
            outboundChannel.pipeline().addLast(WebSocketClientCompressionHandler.INSTANCE);
        }
    }

    /**
     * 判断是否是websocket升级请求
     * @param headers
     * @return
     */
    public static boolean isWebsocketUpgrade(HttpHeaders headers) {
        //this contains check does not allocate an iterator, and most requests are not upgrades
        //so we do the contains check first before checking for specific values
        return headers.contains(HttpHeaderNames.UPGRADE) &&
                headers.containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true) &&
                headers.contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true);
    }
}
