package com.example.nettygatewaydemo.core;

import com.example.nettygatewaydemo.util.ChannelUtils;
import com.example.nettygatewaydemo.util.WsUtils;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import static com.example.nettygatewaydemo.util.AttrKeyConstants.CLIENT_HANDSHAKER_ATTR_KEY;

public class HttpClientInboundHandler extends SimpleChannelInboundHandler {

    private final static Logger logger = LoggerFactory.getLogger(HttpClientInboundHandler.class);

    private Channel inboundChannel;

    public HttpClientInboundHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
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
            inboundChannel.writeAndFlush(((FullHttpMessage)msg).retain()).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        // was able to flush out data, start to read the next chunk

                    } else {
                        future.channel().close();
                    }
                }
            });
        }
        else if (msg instanceof WebSocketFrame) {
            logger.info("收到websocket消息");
            if (msg instanceof TextWebSocketFrame) {
                TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) msg;
                inboundChannel.writeAndFlush(textWebSocketFrame.retain());
            }
            if (msg instanceof PongWebSocketFrame) {
                PongWebSocketFrame frame = (PongWebSocketFrame) msg;
                frame.content().retain();
                inboundChannel.writeAndFlush(new PongWebSocketFrame(frame.content()));
                return;
            }
        } else {
            // logger.info("other");
            if(msg instanceof DefaultHttpContent) {
                if (msg instanceof LastHttpContent) {
                    ChannelFuture channelFuture = inboundChannel.writeAndFlush(((HttpContent)msg).retain()).addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) {
                            if (future.isSuccess()) {
                                logger.info("client write success");
                                // was able to flush out data, start to read the next chunk
                                /*if (!false) {
                                    closeOnFlush(ctx.channel());
                                    closeOnFlush(inboundChannel);
                                }*/
                            } else {
                                future.channel().close();
                            }
                        }
                    });
                } else {
                    inboundChannel.writeAndFlush(((HttpContent)msg).retain());
                }
            } else {
                if (msg instanceof DefaultHttpResponse) {
                    DefaultHttpResponse fullHttpMessage = (DefaultHttpResponse) msg;
                    if (Objects.equals(fullHttpMessage.status(), HttpResponseStatus.SWITCHING_PROTOCOLS)) {
                        WebSocketClientHandshaker webSocketClientHandshaker = ctx.channel().attr(CLIENT_HANDSHAKER_ATTR_KEY).get();
                        if (!webSocketClientHandshaker.isHandshakeComplete()) {
                            try {
                                DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SWITCHING_PROTOCOLS);
                                defaultFullHttpResponse.headers().add(fullHttpMessage.headers());
                                webSocketClientHandshaker.finishHandshake(ctx.channel(), defaultFullHttpResponse);
                                logger.info("WebSocket Client connected!");
                                ((ChannelPromise) this.handshakeFuture()).setSuccess();
                            } catch (WebSocketHandshakeException e) {
                                logger.info("WebSocket Client failed to connect");
                                ((ChannelPromise) this.handshakeFuture()).setFailure(e);
                            }
                            // return;
                        }
                    }
                }
                inboundChannel.writeAndFlush(msg);
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
        if (inboundChannel != null) {
            logger.info("关闭inboundChannel");
            ChannelUtils.closeOnFlush(inboundChannel);
            ChannelUtils.closeOnFlush(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("exceptionCaught", cause);
        ChannelUtils.closeOnFlush(inboundChannel);
        ChannelUtils.closeOnFlush(ctx.channel());
    }
}
