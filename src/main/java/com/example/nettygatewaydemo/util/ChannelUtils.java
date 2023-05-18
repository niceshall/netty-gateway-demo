package com.example.nettygatewaydemo.util;

import com.example.nettygatewaydemo.GatewayProperties;
import com.example.nettygatewaydemo.core.HttpClientInboundHandler;
import com.example.nettygatewaydemo.core.HttpClientFactory;
import com.example.nettygatewaydemo.model.RouteDefine;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.nettygatewaydemo.util.AttrKeyConstants.*;

/**
 * @description: channel工具类
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class ChannelUtils {

    private static final Logger logger = LoggerFactory.getLogger(ChannelUtils.class);

    public static void closeOnFlush(Channel ch) {
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static Channel getExistOrNewClientChannel(ChannelHandlerContext ctx) throws URISyntaxException, InterruptedException {
        Channel connectedClientChannel = ctx.channel().attr(CLIENT_CHANNEL).get();
        return connectedClientChannel;
    }

    public static Channel getExistOrNewClientChannel(ChannelHandlerContext ctx, String remoteHost, int remotePort, EventLoopGroup clientGroup, boolean isKeepAlive, boolean transferEncodingChunked, HttpObjectAggregator clienHttpObjectAggregator) throws URISyntaxException, InterruptedException {
        Channel connectedClientChannel = ctx.channel().attr(CLIENT_CHANNEL).get();
        if (connectedClientChannel != null || remoteHost == null || remotePort == 0) {
            logger.info("get exist client channel");
            connectedClientChannel.attr(CLIENT_CHANNEL_TRANSFER_ENCODING_CHUNKED).set(transferEncodingChunked);
            return connectedClientChannel;
        } else {
            logger.info("create new client channel");
        }
        HttpClientInboundHandler httpClientInboundHd = new HttpClientInboundHandler(ctx.channel());
        Channel channel = HttpClientFactory.getClientChannel(remoteHost, remotePort, ctx.channel(), httpClientInboundHd, clientGroup, clienHttpObjectAggregator);
        channel.attr(CLIENT_CHANNEL_TRANSFER_ENCODING_CHUNKED).set(transferEncodingChunked);
        ctx.channel().attr(CLIENT_CHANNEL).set(channel);
        return channel;
    }

    public static void handleHttpRequest(ChannelHandlerContext ctx, Object msg, GatewayProperties gatewayProperties, EventLoopGroup clientGroup, HttpObjectAggregator clientHttpObjectAggregator) throws URISyntaxException, InterruptedException {
        HttpRequest request = (HttpRequest) msg;

        // 1. 获取路由信息
        List<RouteDefine> routes = gatewayProperties.getRoutes();
        if (CollectionUtils.isEmpty(routes)) {
            ResponseUtils.responseNotFound(ctx, request);
            return;
        }

        String uri = request.uri();
        logger.info("uri = {}", uri);
        String[] split = uri.split("\\?");
        String minPath = split[0];


        // 2. 路由匹配
        List<RouteDefine> collect = routes.stream().filter(route -> Objects.equals(route.getPath(), minPath)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(collect)) {
            ResponseUtils.responseNotFound(ctx, request);
            return;
        }
        RouteDefine routeDefine = collect.get(0);
        String remoteUriString = routeDefine.getUri();
        logger.info("remote uri = {}", remoteUriString);
        URI remoteUri = new URI(remoteUriString);
        String host = remoteUri.getHost();
        int port = remoteUri.getPort();
        String path = routeDefine.getRewritePath() == null ? uri : routeDefine.getRewritePath();
        // TODO split[1]
        String paramPath = split.length > 1 ? split[1] : "";
        if (!StringUtils.isEmpty(paramPath)) {
            path = path + "?" + paramPath;
        }

        String fullRemoteUri = "http://" + host + ":" + port + path;
        String remoteHost = host + ":" + port;

        resetNettyRequest(request, remoteHost, fullRemoteUri);

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        boolean transferEncodingChunked = HttpUtil.isTransferEncodingChunked(request);
        ChannelUtils.getExistOrNewClientChannel(ctx, host, port, clientGroup, keepAlive, transferEncodingChunked, clientHttpObjectAggregator);

    }

    private static void resetNettyRequest(HttpRequest request, String remoteHost, String fullRemoteUri) {
        request.headers().set("host", remoteHost);
        request.setUri(fullRemoteUri);
        logger.info("remote full uri = {}", fullRemoteUri);
    }


}
