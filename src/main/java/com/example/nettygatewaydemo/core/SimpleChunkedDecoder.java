package com.example.nettygatewaydemo.core;

import com.example.nettygatewaydemo.GatewayProperties;
import com.example.nettygatewaydemo.model.RouteDefine;
import com.example.nettygatewaydemo.util.ChannelUtils;
import com.example.nettygatewaydemo.util.ResponseUtils;
import com.example.nettygatewaydemo.util.SpringContextHolder;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SimpleChunkedDecoder extends ChunkedMessageToMessageDecoder<Object> {

    private final static Logger logger = LoggerFactory.getLogger(SimpleChunkedDecoder.class);

    private GatewayProperties gatewayProperties;

    private EventLoopGroup clientGroup;

    private HttpObjectAggregator httpObjectAggregator;
    private HttpObjectAggregator clientHttpObjectAggregator;

    public SimpleChunkedDecoder(EventLoopGroup clientGroup, HttpObjectAggregator httpObjectAggregator, HttpObjectAggregator clientHttpObjectAggregator) {
        this.gatewayProperties = SpringContextHolder.getContext().getBean(GatewayProperties.class);
        this.clientGroup = clientGroup;
        this.httpObjectAggregator = httpObjectAggregator;
        this.clientHttpObjectAggregator = clientHttpObjectAggregator;
    }

    // private final Queue<Object> messageQueue = new ArrayDeque<>();
    private boolean processingMessage = false;

    /**
     *
     * @param ctx           the {@link ChannelHandlerContext} which this {@link io.netty.handler.codec.MessageToMessageDecoder} belongs to
     * @param msg           the message to decode to an other one
     * @param out           the {@link List} to which decoded messages should be added
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        if (processingMessage) {
            // Store message for later processing
//            messageQueue.add(msg);
//            while (!messageQueue.isEmpty() && getExistOrNewOutboundChannel(ctx) != null) {
//                processingMessage = true;
//                Object message = messageQueue.poll();
//                if (message instanceof LastHttpContent) {
//                    processingMessage = false;
//                }
//
//                // Pass the message on to input
//                ctx.executor().execute(() -> {
//                    ctx.fireChannelRead((message));
//                });
//
//                // ctx.fireChannelRead((message));
//            }

            processingMessage = true;
            if (msg instanceof LastHttpContent) {
                processingMessage = false;
            }

            // Pass the message on to input
            ctx.executor().execute(() -> {
                ctx.fireChannelRead((msg));
            });
        } else {

            if (msg instanceof HttpRequest) {
                Boolean aBoolean = handleHttpRequest(ctx, msg);
                if (aBoolean != null && aBoolean) {
                    // Pass data on to input
                    out.add(msg);
                    // Prevent processing of new messages
                    processingMessage = true;
                } else {
                    // Pass data on to input
                    // out.add(msg);
                    httpObjectAggregator.channelRead(ctx, msg);
                    // ctx.pipeline().
                }
            } else if (msg instanceof WebSocketFrame) {
                out.add(msg);
            } else {
                // out.add(msg);
                httpObjectAggregator.channelRead(ctx, msg);
            }
        }
    }

    private Boolean handleHttpRequest(ChannelHandlerContext ctx, Object msg) throws URISyntaxException, InterruptedException {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            String contentTypeStr = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
            boolean isMultipart = contentTypeStr != null ? contentTypeStr.contains(HttpHeaders.Values.MULTIPART_FORM_DATA) : false;

            // 1. 获取路由信息
            List<RouteDefine> routes = gatewayProperties.getRoutes();
            if (CollectionUtils.isEmpty(routes)) {
                FullHttpResponse fullHttpResponse = ResponseUtils.creat404(request);
                ctx.writeAndFlush(fullHttpResponse.retain()).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        ChannelUtils.closeOnFlush(ctx.channel());
                    }
                });
                return null;
            }

            String uri = request.uri();
            logger.info("uri = {}", uri);
            String[] split = uri.split("\\?");
            String minPath = split[0];


            // 2. 路由匹配
            List<RouteDefine> collect = routes.stream().filter(route -> Objects.equals(route.getPath(), minPath)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(collect)) {
                FullHttpResponse fullHttpResponse = ResponseUtils.creat404(request);
                ctx.writeAndFlush(fullHttpResponse.retain()).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        ChannelUtils.closeOnFlush(ctx.channel());
                    }
                });
                return null;
            }
            RouteDefine routeDefine = collect.get(0);
            String remoteUriString = routeDefine.getUri();
            logger.info("remote uri = {}", remoteUriString);
            URI remoteUri = new URI(remoteUriString);
            String host = remoteUri.getHost();
            int port = remoteUri.getPort();
            String path = routeDefine.getRewritePath() == null ? uri : routeDefine.getRewritePath();

            boolean keepAlive = HttpUtil.isKeepAlive(request);
            boolean transferEncodingChunked = HttpUtil.isTransferEncodingChunked(request);
            ChannelUtils.getExistOrNewOutboundChannel(ctx, host, port, clientGroup, keepAlive, transferEncodingChunked, clientHttpObjectAggregator);

            // TODO split[1]
            String paramPath = split.length > 1 ? split[1] : "";
            if (!StringUtils.isEmpty(paramPath)) {
                path = path + "?" + paramPath;
            }

            String fullRemoteUri = "http://" + host + ":" + port + path;
            request.headers().set("host", host + ":" + port);
            request.setUri(fullRemoteUri);
            logger.info("remote full uri = {}", fullRemoteUri);


            // MULTIPART_FORM_DATA_VALUE
            return isMultipart;

        }
        return null;
    }
}
