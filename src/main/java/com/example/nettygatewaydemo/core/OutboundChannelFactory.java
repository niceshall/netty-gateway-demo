package com.example.nettygatewaydemo.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;

public class OutboundChannelFactory {

    private final static Logger logger = LoggerFactory.getLogger(OutboundChannelFactory.class);


    public static Channel outboundChannel(String address, int port, Channel inboundChannel, HttpClientInboundHandler httpClientInboundHandler, EventLoopGroup group) throws URISyntaxException, InterruptedException {
        // EventLoopGroup group2 = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();

        bootstrap
                .group(group)
                .channel(inboundChannel.getClass())
                // reuse event loop
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpClientCodec());
                        // pipeline.addLast(new HttpObjectAggregator(8192));
                        // pipeline.addLast(WebSocketClientCompressionHandler.INSTANCE);
                        // pipeline.addLast(new LoggingHandler(LogLevel.INFO));
                        // pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(httpClientInboundHandler);
                    }
                })
        // read is initiated by backend handler
        .option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture f = bootstrap.connect(address, port);
        Channel ch = f.sync().channel();
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    logger.info("connect to remote server success");
                    // connection complete start to read first data
                    inboundChannel.config().setAutoRead(true);
                    inboundChannel.read();
                    ch.config().setAutoRead(true);
                } else {
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                    logger.error("connect to remote server failed");
                }
            }
        });
        return ch;

    }
}
