package com.example.nettygatewaydemo.util;

import com.example.nettygatewaydemo.core.HttpClientInboundHandler;
import com.example.nettygatewaydemo.core.HttpClientFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

import static com.example.nettygatewaydemo.util.AttrKeyConstants.*;

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

    public static Channel getExistOrNewOutboundChannel(ChannelHandlerContext ctx, String remoteHost, int remotePort, EventLoopGroup clientGroup, boolean isKeepAlive, boolean transferEncodingChunked, HttpObjectAggregator clienHttpObjectAggregator) throws URISyntaxException, InterruptedException {
        Channel connectedClientChannel = ctx.channel().attr(CLIENT_CHANNEL).get();
        if (connectedClientChannel != null || remoteHost == null || remotePort == 0) {
            logger.info("get exist outbound channel");
            connectedClientChannel.attr(CLIENT_CHANNEL_TRANSFER_ENCODING_CHUNKED).set(transferEncodingChunked);
            return connectedClientChannel;
        } else {
            logger.info("create new outbound channel");
        }
        HttpClientInboundHandler httpClientInboundHd = new HttpClientInboundHandler(ctx.channel());
        Channel channel = HttpClientFactory.outboundChannel(remoteHost, remotePort, ctx.channel(), httpClientInboundHd, clientGroup, clienHttpObjectAggregator);
        channel.attr(CLIENT_CHANNEL_TRANSFER_ENCODING_CHUNKED).set(transferEncodingChunked);
        ctx.channel().attr(CLIENT_CHANNEL).set(channel);
        return channel;
    }


}
