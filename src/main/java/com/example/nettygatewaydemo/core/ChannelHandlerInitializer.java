package com.example.nettygatewaydemo.core;

import com.example.nettygatewaydemo.util.PropertiesConfigUtils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

public class ChannelHandlerInitializer extends ChannelInitializer<SocketChannel> {

    // eventLoopGroup for server connect to backend server
    private EventLoopGroup clientGroup = new NioEventLoopGroup();

    private static int taskGroupCoreSize;

    private static EventExecutorGroup workExecutor;


    static {
        try {
            taskGroupCoreSize = Integer.parseInt(PropertiesConfigUtils.getProperty("netty.taskgroup.coreSize"));
        } catch (Exception e) {
            taskGroupCoreSize = 100;
        }
        // netty自带Unordered工作线程池
        workExecutor = new UnorderedThreadPoolEventExecutor(taskGroupCoreSize, new DefaultThreadFactory("gateway-taskGroup"));
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 1. http编解码器
        pipeline.addLast(new HttpServerCodec());
        // 打印详细日志
        // pipeline.addLast(new LoggingHandler(LogLevel.INFO));
        // pipeline.addLast(new ChunkedWriteHandler());
        // 3. 自定义处理器;
        // pipeline.addLast(new TestChannelHandler());
        HttpObjectAggregator httpObjectAggregator = new HttpObjectAggregator(1022 * 1024 * 10);
        pipeline.addLast("queuingDecoderChunked", new QueuingDecoderChunked(clientGroup, httpObjectAggregator));
        pipeline.addLast(new HttpServerHandler());
        // pipeline.addLast(workExecutor, new HttpServerInboundHandler());

    }
}
