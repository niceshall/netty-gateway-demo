package com.example.nettygatewaydemo.core;

import com.example.nettygatewaydemo.GatewayProperties;
import com.example.nettygatewaydemo.netty.UnReleaseMessageToMessageDecoder;
import com.example.nettygatewaydemo.util.ChannelUtils;
import com.example.nettygatewaydemo.util.SpringContextHolder;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @description: http服务端处理器
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class SimpleChunkedDecoderHandler extends UnReleaseMessageToMessageDecoder<Object> {

    private final static Logger logger = LoggerFactory.getLogger(SimpleChunkedDecoderHandler.class);

    private GatewayProperties gatewayProperties;

    private EventLoopGroup clientGroup;

    private HttpObjectAggregator httpObjectAggregator;
    private HttpObjectAggregator clientHttpObjectAggregator;

    public SimpleChunkedDecoderHandler(GatewayProperties gatewayProperties, EventLoopGroup clientGroup, HttpObjectAggregator httpObjectAggregator, HttpObjectAggregator clientHttpObjectAggregator) {
        this.gatewayProperties = gatewayProperties;
        this.clientGroup = clientGroup;
        this.httpObjectAggregator = httpObjectAggregator;
        this.clientHttpObjectAggregator = clientHttpObjectAggregator;
    }

    private boolean processingMessage = false;

    /**
     * @param ctx the {@link ChannelHandlerContext} which this {@link io.netty.handler.codec.MessageToMessageDecoder} belongs to
     * @param msg the message to decode to an other one
     * @param out the {@link List} to which decoded messages should be added
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        if (processingMessage) {
            if (msg instanceof LastHttpContent) {
                processingMessage = false;
            }

            // Pass the message on to input
            ctx.executor().execute(() -> {
                ctx.fireChannelRead((msg));
            });
        } else {

            if (msg instanceof HttpRequest) {
                ChannelUtils.handleHttpRequest(ctx, msg, gatewayProperties, clientGroup, clientHttpObjectAggregator);
                HttpRequest request = (HttpRequest) msg;
                String contentTypeStr = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
                boolean isMultipart = contentTypeStr != null && contentTypeStr.contains(HttpHeaders.Values.MULTIPART_FORM_DATA);
                if (isMultipart) {
                    // Pass data on to input
                    out.add(msg);
                    // Prevent processing of new messages
                    processingMessage = true;
                } else {
                    // Pass data on to input
                    httpObjectAggregator.channelRead(ctx, msg);
                }
            } else if (msg instanceof WebSocketFrame) {
                out.add(msg);
            } else {
                // out.add(msg);
                httpObjectAggregator.channelRead(ctx, msg);
            }
        }
    }


}
