package com.example.nettygatewaydemo.core;

import com.example.nettygatewaydemo.util.PropertiesConfigUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @description: http服务器
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private int portHTTP = 18088;
    private int childOptionBuffSize = 65535;
    private static HttpServer httpServer = new HttpServer();

    private HttpServer() {

        try {
            String nettyGatewayPortStre = PropertiesConfigUtils.getProperty("netty.gateway.port");
            portHTTP = Integer.valueOf(nettyGatewayPortStre);
        } catch (Exception e) {
            portHTTP = 18088;
        }
    }

    private void initEventLoopGroup() {
        // bossGroup 用于接收连接 设置线程数为1
        // workerGroup 用于处理连接 不设置线程数 默认为cpu核数*2
        bossGroup = new NioEventLoopGroup(4);
        workerGroup = new NioEventLoopGroup();
        logger.info("bossGroup and workerGroup init success");
    }

    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;

    public static HttpServer getInstance() {
        return httpServer;
    }

    public void start() {
        // 初始化线程组
        initEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        try {
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(this.portHTTP))
                    .option(ChannelOption.SO_BACKLOG, 1024) // 设置TCP缓冲区
                    .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT) // 设置接收缓冲区大小,默认值为AdaptiveRecvByteBufAllocator.DEFAULT
                    .childOption(ChannelOption.TCP_NODELAY, true) // 设置不延迟，消息立即发送
                    .childOption(ChannelOption.SO_RCVBUF, childOptionBuffSize) // 设置接收缓冲区大小
                    .childOption(ChannelOption.SO_SNDBUF, childOptionBuffSize) // 设置发送缓冲区大小
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // 保持连接
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelHandlerInitializer())
                    .childOption(ChannelOption.AUTO_READ, true);
            logger.info("HttpServer start");
            // 绑定端口
            ChannelFuture fHttp = b.bind(this.portHTTP).sync();
            logger.info("HttpsServer name is " + HttpServer.class.getName() + " started and listen on " + fHttp
                    .channel().localAddress());
            fHttp.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error("启动服务失败");
            logger.error(e.getMessage());
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        try {
            if (null != bossGroup) {
                bossGroup.shutdownGracefully();
            }
            if (null != workerGroup) {
                workerGroup.shutdownGracefully();
            }
            logger.info("关闭服务成功");
        } catch (Exception e) {
            logger.error("关闭服务失败");
            logger.error(e.getMessage());
        }
    }


}
