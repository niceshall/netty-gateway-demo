package com.example.nettygatewaydemo.util;

import com.example.nettygatewaydemo.core.HttpClientInboundHandler;
import com.example.nettygatewaydemo.core.OutboundChannelFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

import static com.example.nettygatewaydemo.util.AttrKeyConstants.CLIENT_CHANNEL;
import static com.example.nettygatewaydemo.util.AttrKeyConstants.CLIENT_CHANNEL_KEEP_ALIVE;

public class ChannelUtils {

    private static final Logger logger = LoggerFactory.getLogger(ChannelUtils.class);

    public static void closeOnFlush(Channel ch) {
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static Channel getExistOrNewOutboundChannel(ChannelHandlerContext ctx) throws URISyntaxException, InterruptedException {
        Channel connectedClientChannel = ctx.channel().attr(CLIENT_CHANNEL).get();
        return connectedClientChannel;
    }

    public static Channel getExistOrNewOutboundChannel(ChannelHandlerContext ctx, String remoteHost, int remotePort, EventLoopGroup clientGroup, boolean isKeepAlive) throws URISyntaxException, InterruptedException {
        Channel connectedClientChannel = ctx.channel().attr(CLIENT_CHANNEL).get();
        if (connectedClientChannel != null || remoteHost == null || remotePort == 0) {
            logger.info("get exist outbound channel");
            return connectedClientChannel;
        } else {
            logger.info("create new outbound channel");
        }
        HttpClientInboundHandler httpClientInboundHd = new HttpClientInboundHandler(ctx.channel());
        Channel channel = OutboundChannelFactory.outboundChannel(remoteHost, remotePort, ctx.channel(), httpClientInboundHd, clientGroup);
        channel.attr(CLIENT_CHANNEL_KEEP_ALIVE).set(isKeepAlive);
        ctx.channel().attr(CLIENT_CHANNEL).set(channel);
        return channel;
    }


}
