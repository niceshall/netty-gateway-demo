package com.example.nettygatewaydemo.core.handler;

import com.example.nettygatewaydemo.util.AttrKeyConstants;
import com.example.nettygatewaydemo.util.ChannelUtils;
import io.netty.channel.*;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @description: http客户端处理器
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class HttpClientInboundHandler extends SimpleChannelInboundHandler {

    private final static Logger logger = LoggerFactory.getLogger(HttpClientInboundHandler.class);

    private ChannelPromise handshakeFuture;

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        Channel serverChannel = ctx.channel().attr(AttrKeyConstants.SERVER_CHANNEL).get();
        if (msg instanceof FullHttpMessage) {
            FullHttpResponse fullHttpResponse = (FullHttpResponse) msg;
            if (Objects.equals(fullHttpResponse.status(), HttpResponseStatus.SWITCHING_PROTOCOLS)) {
                finishWebSocketClientHandshaker(ctx, fullHttpResponse);
            } else {
                doFinishedServerResponse(ctx, fullHttpResponse, serverChannel);
            }
        } else if (msg instanceof WebSocketFrame) {
            logger.info("收到websocket消息");
            if (msg instanceof TextWebSocketFrame) {
                TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) msg;
                serverChannel.writeAndFlush(textWebSocketFrame.retain());
            }
            if (msg instanceof PongWebSocketFrame) {
                PongWebSocketFrame frame = (PongWebSocketFrame) msg;
                frame.content().retain();
                serverChannel.writeAndFlush(new PongWebSocketFrame(frame.content()));
                return;
            }
        } else {
            if (msg instanceof HttpContent) {
                if (msg instanceof LastHttpContent) {
                    doFinishedServerResponse(ctx, (HttpContent) msg, serverChannel);
                } else {
                    serverChannel.writeAndFlush(((HttpContent) msg).retain());
                }
            } else {
                serverChannel.writeAndFlush(msg);
            }
        }

    }

    /**
     * 完成http客户端响应
     * @param ctx
     * @param msg
     * @param serverChannel
     */
    private static void doFinishedServerResponse(ChannelHandlerContext ctx, HttpContent msg, Channel serverChannel) {
        serverChannel.writeAndFlush(msg.retain()).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    logger.info("client write success");
                    // was able to flush out data, start to read the next chunk
                    SimpleChannelPool simpleChannelPool = ctx.channel().attr(AttrKeyConstants.CLIENT_POOL).get();
                    simpleChannelPool.release(ctx.channel());
                } else {
                    future.channel().close();
                }
            }
        });
    }

    /**
     * 完成websocket客户端握手
     * @param ctx
     * @param fullHttpResponse
     */
    private void finishWebSocketClientHandshaker(ChannelHandlerContext ctx, FullHttpResponse fullHttpResponse) {
        WebSocketClientHandshaker webSocketClientHandshaker = ctx.channel().attr(AttrKeyConstants.CLIENT_HANDSHAKER_ATTR_KEY).get();
        if (!webSocketClientHandshaker.isHandshakeComplete()) {
            try {
                webSocketClientHandshaker.finishHandshake(ctx.channel(), fullHttpResponse);
                logger.info("WebSocket Client connected!");
                ((ChannelPromise) this.handshakeFuture()).setSuccess();
            } catch (WebSocketHandshakeException e) {
                logger.info("WebSocket Client failed to connect");
                ((ChannelPromise) this.handshakeFuture()).setFailure(e);
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ctx.read();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel serverChannel = ctx.channel().attr(AttrKeyConstants.SERVER_CHANNEL).get();
        if (serverChannel != null) {
            logger.info("关闭serverChannel");
            ChannelUtils.closeOnFlush(serverChannel);
            ChannelUtils.closeOnFlush(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("exceptionCaught", cause);
        Channel serverChannel = ctx.channel().attr(AttrKeyConstants.SERVER_CHANNEL).get();
        ChannelUtils.closeOnFlush(serverChannel);
        ChannelUtils.closeOnFlush(ctx.channel());
    }
}
