package com.example.nettygatewaydemo.core;


import com.example.nettygatewaydemo.util.ChannelUtils;
import com.example.nettygatewaydemo.util.WsUtils;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.nettygatewaydemo.util.AttrKeyConstants.CLIENT_CHANNEL_KEEP_ALIVE;


public class HttpServerHandler extends SimpleChannelInboundHandler {

    private final static Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel outboundChannel = ChannelUtils.getExistOrNewOutboundChannel(ctx);
        if (msg instanceof HttpContent) {
            if (msg instanceof LastHttpContent) {
                ChannelFuture channelFuture = outboundChannel.writeAndFlush(((HttpContent) msg).retain());
            } else {
                outboundChannel.writeAndFlush(((HttpContent)msg).retain());
            }
        } else if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            HttpHeaders headers = request.headers();
            boolean websocketUpgrade = WsUtils.isWebsocketUpgrade(headers);
            if (websocketUpgrade) {
                logger.info("websocket upgrade");
                logger.info("request uri: {}", request.uri());
                String fullRemoteUri = request.getUri();
                if (WsUtils.wsServerHandshakeWithClient(ctx, request, fullRemoteUri, outboundChannel)) return;
            }

            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                request.headers().set("connection", "keep-alive");
            }
            outboundChannel.writeAndFlush(msg);
        }

        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) msg;
            logger.info("text, {}", textWebSocketFrame.text());
            outboundChannel.writeAndFlush(textWebSocketFrame.retain());
            return;
        }
        if (msg instanceof PingWebSocketFrame) {
            PingWebSocketFrame frame = (PingWebSocketFrame) msg;
            frame.content().retain();
            outboundChannel.writeAndFlush(new PingWebSocketFrame(frame.content()));
            readIfNeeded(ctx);
            return;
        }
        if (msg instanceof PongWebSocketFrame && true) {
            PongWebSocketFrame frame = (PongWebSocketFrame) msg;
            readIfNeeded(ctx);
            return;
        }
        if (msg instanceof CloseWebSocketFrame) {
            CloseWebSocketFrame frame = (CloseWebSocketFrame) msg;
            outboundChannel.writeAndFlush(frame.retain());
            ChannelUtils.closeOnFlush(outboundChannel);
            return;
        }
    }

    private static void readIfNeeded(ChannelHandlerContext ctx) {
        if (!ctx.channel().config().isAutoRead()) {
            ctx.read();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel existOrNewOutboundChannel = ChannelUtils.getExistOrNewOutboundChannel(ctx);
        if (existOrNewOutboundChannel != null) {
            logger.info("关闭outboundChannel");
            ChannelUtils.closeOnFlush(existOrNewOutboundChannel);
            ChannelUtils.closeOnFlush(ctx.channel());
        }
    }
}
