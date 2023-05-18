package com.example.nettygatewaydemo.core;

import com.example.nettygatewaydemo.util.ChannelUtils;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static com.example.nettygatewaydemo.util.AttrKeyConstants.CLIENT_HANDSHAKER_ATTR_KEY;

/**
 * @description: http客户端处理器
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class HttpClientInboundHandler extends SimpleChannelInboundHandler {

    private final static Logger logger = LoggerFactory.getLogger(HttpClientInboundHandler.class);

    private Channel serverChannel;

    public HttpClientInboundHandler(Channel serverChannel) {
        this.serverChannel = serverChannel;
    }

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
        if (msg instanceof FullHttpMessage) {
            FullHttpResponse fullHttpResponse = (FullHttpResponse) msg;
            if (Objects.equals(fullHttpResponse.status(), HttpResponseStatus.SWITCHING_PROTOCOLS)) {
                WebSocketClientHandshaker webSocketClientHandshaker = ctx.channel().attr(CLIENT_HANDSHAKER_ATTR_KEY).get();
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
            } else {
                serverChannel.writeAndFlush(((FullHttpMessage) msg).retain()).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {

                        } else {
                            future.channel().close();
                        }
                    }
                });
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
                    serverChannel.writeAndFlush(((HttpContent) msg).retain()).addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) {
                            if (future.isSuccess()) {
                                logger.info("client write success");
                                // was able to flush out data, start to read the next chunk
                            } else {
                                future.channel().close();
                            }
                        }
                    });
                } else {
                    serverChannel.writeAndFlush(((HttpContent) msg).retain());
                }
            } else {
                serverChannel.writeAndFlush(msg);
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
        if (serverChannel != null) {
            logger.info("关闭serverChannel");
            ChannelUtils.closeOnFlush(serverChannel);
            ChannelUtils.closeOnFlush(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("exceptionCaught", cause);
        ChannelUtils.closeOnFlush(serverChannel);
        ChannelUtils.closeOnFlush(ctx.channel());
    }
}
