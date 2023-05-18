package com.example.nettygatewaydemo.core;


import com.example.nettygatewaydemo.GatewayProperties;
import com.example.nettygatewaydemo.util.ChannelUtils;
import com.example.nettygatewaydemo.util.WsUtils;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description: http服务端处理器
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class HttpServerHandler extends SimpleChannelInboundHandler {

    private final static Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel clientChannel = ChannelUtils.getExistOrNewClientChannel(ctx);
        if (clientChannel == null) {
            logger.error("clientChannel is null");
            if (msg instanceof HttpContent) {
            }
            return;
        }
        if (msg instanceof HttpContent) {
            // 已聚合http请求
            if (msg instanceof FullHttpRequest) {
                HttpRequest request = (HttpRequest) msg;
                HttpHeaders headers = request.headers();
                boolean websocketUpgrade = WsUtils.isWebsocketUpgrade(headers);
                if (websocketUpgrade) {
                    logger.info("websocket upgrade");
                    logger.info("request uri: {}", request.uri());
                    String fullRemoteUri = request.getUri();
                    if (WsUtils.wsServerHandshakeWithClient(ctx, request, fullRemoteUri, clientChannel)) return;
                }

                boolean keepAlive = HttpUtil.isKeepAlive(request);
                if (keepAlive) {
                    request.headers().set("connection", "keep-alive");
                }
            }
            clientChannel.writeAndFlush(((HttpContent) msg).retain());
        } else if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                request.headers().set("connection", "keep-alive");
            }
            clientChannel.writeAndFlush(msg);
        }

        // 处理websocket
        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) msg;
            logger.info("text, {}", textWebSocketFrame.text());
            clientChannel.writeAndFlush(textWebSocketFrame.retain());
            return;
        }
        if (msg instanceof PingWebSocketFrame) {
            PingWebSocketFrame frame = (PingWebSocketFrame) msg;
            frame.content().retain();
            clientChannel.writeAndFlush(new PingWebSocketFrame(frame.content()));
            return;
        }
        if (msg instanceof PongWebSocketFrame && true) {
            PongWebSocketFrame frame = (PongWebSocketFrame) msg;
            return;
        }
        if (msg instanceof CloseWebSocketFrame) {
            CloseWebSocketFrame frame = (CloseWebSocketFrame) msg;
            clientChannel.writeAndFlush(frame.retain());
            ChannelUtils.closeOnFlush(clientChannel);
            return;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel existOrNewOutboundChannel = ChannelUtils.getExistOrNewClientChannel(ctx);
        if (existOrNewOutboundChannel != null) {
            logger.info("关闭clientChannel");
            ChannelUtils.closeOnFlush(existOrNewOutboundChannel);
            ChannelUtils.closeOnFlush(ctx.channel());
        }
    }
}
