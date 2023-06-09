package com.example.nettygatewaydemo.core.handler;

import com.example.nettygatewaydemo.GatewayProperties;
import com.example.nettygatewaydemo.util.PropertiesConfigUtils;
import com.example.nettygatewaydemo.util.SpringContextHolder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

/**
 * @description: 网关配置类
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class ChannelHandlerInitializer extends ChannelInitializer<SocketChannel> {

    private static int taskGroupCoreSize;

    private static EventExecutorGroup workExecutor;

    private static GatewayProperties gatewayProperties;


    static {
        gatewayProperties = SpringContextHolder.getContext().getBean(GatewayProperties.class);
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
        HttpObjectAggregator httpObjectAggregator = new HttpObjectAggregator(1024 * 1024 * 10);
        pipeline.addLast(new FlowControlHandler());
        pipeline.addLast("simpleChunkedDecoderHandler", new SimpleChunkedDecoderHandler(gatewayProperties, httpObjectAggregator));
        pipeline.addLast(workExecutor, new HttpServerHandler());

    }
}
