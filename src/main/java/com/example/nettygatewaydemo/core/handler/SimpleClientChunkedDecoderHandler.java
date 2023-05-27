package com.example.nettygatewaydemo.core.handler;


import com.example.nettygatewaydemo.netty.UnReleaseMessageToMessageDecoder;
import com.example.nettygatewaydemo.util.AttrKeyConstants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @description: http客户端处理器
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class SimpleClientChunkedDecoderHandler extends UnReleaseMessageToMessageDecoder<Object> {

    private final static Logger logger = LoggerFactory.getLogger(SimpleClientChunkedDecoderHandler.class);

    private HttpObjectAggregator httpObjectAggregator;

    public SimpleClientChunkedDecoderHandler(HttpObjectAggregator httpObjectAggregator) {
        this.httpObjectAggregator = httpObjectAggregator;
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
            // Store message for later processing

            processingMessage = true;
            if (msg instanceof LastHttpContent) {
                processingMessage = false;
            }

            // Pass the message on to input
            ctx.executor().execute(() -> {
                ctx.fireChannelRead((msg));
            });
        } else {

            if (msg instanceof HttpResponse) {
                Boolean aBoolean = ctx.channel().attr(AttrKeyConstants.CLIENT_CHANNEL_TRANSFER_ENCODING_CHUNKED).get();
                if (aBoolean) {
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
                httpObjectAggregator.channelRead(ctx, msg);
            }
        }
    }
}
