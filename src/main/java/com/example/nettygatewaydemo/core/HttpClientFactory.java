package com.example.nettygatewaydemo.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

/**
 * @description: http客户端工厂类
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class HttpClientFactory {

    private final static Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);


    public static Channel getClientChannel(String address, int port, Channel inboundChannel, HttpClientInboundHandler httpClientInboundHandler, EventLoopGroup group, HttpObjectAggregator clienHttpObjectAggregator) throws URISyntaxException, InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .group(group)
                .channel(inboundChannel.getClass())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpClientCodec());
                        // pipeline.addLast(new LoggingHandler(LogLevel.INFO));
                        pipeline.addLast(new SimpleClientChunkedDecoderHandler(clienHttpObjectAggregator));
                        pipeline.addLast(httpClientInboundHandler);
                    }
                })
                // read is initiated by backend handler
                //.option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 默认值30000毫秒即30秒
                .option(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture f = bootstrap.connect(address, port);
        Channel ch = f.sync().channel();
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    logger.info("connect to remote server success");
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
