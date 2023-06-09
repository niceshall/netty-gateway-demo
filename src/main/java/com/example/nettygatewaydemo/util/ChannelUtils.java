package com.example.nettygatewaydemo.util;

import com.example.nettygatewaydemo.GatewayProperties;
import com.example.nettygatewaydemo.core.HttpClient;
import com.example.nettygatewaydemo.core.request.NettyClientHttpRequest;
import com.example.nettygatewaydemo.model.RouteDefine;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.concurrent.Future;
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

    public static Future<Channel> getExistOrNewClientChannel(ChannelHandlerContext ctx, URI uri, boolean transferEncodingChunked, RouteDefine routeDefine) throws URISyntaxException, InterruptedException {
        NettyClientHttpRequest nettyClientHttpRequest = new NettyClientHttpRequest(routeDefine, uri, null);
        return HttpClient.getClientChannel(nettyClientHttpRequest, ctx.channel(), transferEncodingChunked, ctx);
    }

    public static void handleHttpRequest(ChannelHandlerContext ctx, Object msg, GatewayProperties gatewayProperties) throws URISyntaxException, InterruptedException {
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
        URI simpleRemoteUri = getUri(request, uri, split, routeDefine);
        boolean transferEncodingChunked = HttpUtil.isTransferEncodingChunked(request);
        ChannelUtils.getExistOrNewClientChannel(ctx, simpleRemoteUri, transferEncodingChunked, routeDefine);

    }

    private static URI getUri(HttpRequest request, String uri, String[] split, RouteDefine routeDefine) throws URISyntaxException {
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

        URI simpleRemoteUri = new URI("http://" + remoteHost);

        resetNettyRequest(request, remoteHost, fullRemoteUri);
        return simpleRemoteUri;
    }

    private static void resetNettyRequest(HttpRequest request, String remoteHost, String fullRemoteUri) {
        request.headers().set("host", remoteHost);
        request.setUri(fullRemoteUri);
        logger.info("remote full uri = {}", fullRemoteUri);
    }


}
