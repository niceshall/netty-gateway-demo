package com.example.nettygatewaydemo.core.handler;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description: NettyClientPoolHandler
 * @create: 2022/5/19
 */
public class NettyClientPoolHandler implements ChannelPoolHandler {

    private static final Logger log = LoggerFactory.getLogger(NettyClientPoolHandler.class);
    @Override
    public void channelReleased(Channel ch) throws Exception {
        log.info("channelReleased. Channel_ID is {}", ch.id());
    }

    @Override
    public void channelAcquired(Channel ch) throws Exception {
        log.info("channelAcquired. Channel_ID is {}", ch.id());
    }

    @Override
    public void channelCreated(Channel ch) throws Exception {
        log.info("channelCreated. Channel_ID is {}", ch.id());
        NioSocketChannel socketChannel = (NioSocketChannel) ch;
        HttpObjectAggregator clientHttpObjectAggregator = new HttpObjectAggregator(1024 * 1024 * 10);
        socketChannel.pipeline()
                .addLast(new HttpClientCodec())
                .addLast(new SimpleClientChunkedDecoderHandler(clientHttpObjectAggregator))
//                .addLast("ReadTimeoutHandler",new ReadTimeoutHandler(30))
//                .addLast("WriteTimeoutHandler",new WriteTimeoutHandler(5))
                .addLast(new HttpClientInboundHandler());
    }
}
