package com.example.nettygatewaydemo.core;

import com.example.nettygatewaydemo.core.handler.NettyClientPoolHandler;
import com.example.nettygatewaydemo.core.request.NettyClientHttpRequest;
import com.example.nettygatewaydemo.util.AttrKeyConstants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;

/**
 * @description: http客户端工厂类
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class HttpClient {

    private final static Logger logger = LoggerFactory.getLogger(HttpClient.class);


    private static final EventLoopGroup group = new NioEventLoopGroup(4);
    private static final Bootstrap bootstrap = new Bootstrap();
    private static ChannelPoolMap<InetSocketAddress, SimpleChannelPool> channelPoolMap;


    public HttpClient() {

    }

    static {
        bootstrap
                .group(group)
                .channel(NioSocketChannel.class)
                // read is initiated by backend handler
                //.option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 默认值30000毫秒即30秒
                .option(ChannelOption.SO_KEEPALIVE, true);
        channelPoolMap = new AbstractChannelPoolMap<InetSocketAddress, SimpleChannelPool>() {
            @Override
            protected SimpleChannelPool newPool(InetSocketAddress key) {
                return new FixedChannelPool(bootstrap.remoteAddress(key), new NettyClientPoolHandler(), 1000, 10000);
            }
        };
    }


    public static Future<Channel> getClientChannel(NettyClientHttpRequest httpRequest, Channel serverChannel, boolean transferEncodingChunked, ChannelHandlerContext ctx) throws URISyntaxException, InterruptedException {
        SimpleChannelPool pool = channelPoolMap.get(httpRequest.getSocketAddress());

        Future<Channel> channelFuture = pool.acquire().sync();
        serverChannel.attr(AttrKeyConstants.CLIENT_CHANNEL_F).set(channelFuture);
        serverChannel.attr(AttrKeyConstants.CLIENT_POOL).set(pool);
        return channelFuture;
    }
}
