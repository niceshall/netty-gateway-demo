package com.example.nettygatewaydemo.core.handler;


import com.example.nettygatewaydemo.util.AttrKeyConstants;
import com.example.nettygatewaydemo.util.ChannelUtils;
import com.example.nettygatewaydemo.util.WsUtils;
import io.netty.channel.*;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
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
        Future<Channel> channelFuture = ctx.channel().attr(AttrKeyConstants.CLIENT_CHANNEL_F).get();
        Channel clientChannel = ChannelUtils.getExistOrNewClientChannel(ctx);
        if (msg instanceof HttpRequest) {
            if (channelFuture == null) {
                logger.error("channelFuture is null");
                return;
            }
            HttpRequest request = (HttpRequest) msg;
            boolean websocketUpgrade = isWebsocketUpgrade(request);
            // 已聚合http请求
            if (msg instanceof FullHttpRequest) {
                msg = ((HttpContent) msg).retain();
            }
            Object finalMsg = msg;
            doClientRequest(ctx, channelFuture, request, websocketUpgrade, finalMsg);
            return;
        }
        if (msg instanceof HttpContent) {
            clientChannel.writeAndFlush(((HttpContent) msg).retain());
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

    private static boolean isWebsocketUpgrade(HttpRequest request) {
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        HttpHeaders headers = request.headers();
        boolean websocketUpgrade = WsUtils.isWebsocketUpgrade(headers);
        if (keepAlive && !websocketUpgrade) {
            request.headers().set("connection", "keep-alive");
        }
        return websocketUpgrade;
    }

    private static void doClientRequest(ChannelHandlerContext ctx, Future<Channel> channelFuture, HttpRequest request, boolean websocketUpgrade, Object finalMsg) {
        boolean transferEncodingChunked = HttpUtil.isTransferEncodingChunked(request);
        SimpleChannelPool pool = ctx.channel().attr(AttrKeyConstants.CLIENT_POOL).get();
        channelFuture.addListener(new FutureListener<Channel>() {
            @Override
            public void operationComplete(Future<Channel> future) throws Exception {
                ctx.channel().config().setAutoRead(true);
                ctx.channel().read();
                if (future.isSuccess()) {
                    Channel clientChannel = future.getNow();
                    ctx.channel().attr(AttrKeyConstants.CLIENT_CHANNEL).set(clientChannel);
                    clientChannel.attr(AttrKeyConstants.SERVER_CHANNEL).set(ctx.channel());
                    clientChannel.attr(AttrKeyConstants.CLIENT_POOL).set(pool);
                    clientChannel.attr(AttrKeyConstants.CLIENT_CHANNEL_TRANSFER_ENCODING_CHUNKED).set(transferEncodingChunked);
                    if (clientChannel != null) {
                        if (websocketUpgrade) {
                            logger.info("websocket upgrade");
                            logger.info("request uri: {}", request.uri());
                            String fullRemoteUri = request.getUri();
                            if (WsUtils.wsServerHandshakeWithClient(ctx, request, fullRemoteUri, clientChannel)) return;
                        }
                        clientChannel.writeAndFlush(finalMsg);
                    }
                } else {
                    ctx.channel().config().setAutoRead(true);
                }
            }
        });
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("exceptionCaught", cause);
        ChannelUtils.closeOnFlush(ctx.channel());
    }
}
