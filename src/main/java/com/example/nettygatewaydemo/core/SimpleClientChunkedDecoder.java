package com.example.nettygatewaydemo.core;


import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import static com.example.nettygatewaydemo.util.AttrKeyConstants.CLIENT_CHANNEL_TRANSFER_ENCODING_CHUNKED;

public class SimpleClientChunkedDecoder extends ChunkedMessageToMessageDecoder<Object> {

    private final static Logger logger = LoggerFactory.getLogger(SimpleClientChunkedDecoder.class);

    private HttpObjectAggregator httpObjectAggregator;

    public SimpleClientChunkedDecoder(HttpObjectAggregator httpObjectAggregator) {
        this.httpObjectAggregator = httpObjectAggregator;
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
                Boolean aBoolean = ctx.channel().attr(CLIENT_CHANNEL_TRANSFER_ENCODING_CHUNKED).get();
                if (aBoolean) {
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
}
