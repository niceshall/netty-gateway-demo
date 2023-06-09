package com.example.nettygatewaydemo.util;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * @description: response工具类
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class ResponseUtils {

    static public FullHttpResponse createDefaultResponse() {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, OK,
                Unpooled.wrappedBuffer(Unpooled.EMPTY_BUFFER));
        response.headers()
                .set(CONTENT_TYPE, APPLICATION_JSON)
                .setInt(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(CONNECTION, KEEP_ALIVE);
        return response;
    }

    static public FullHttpResponse createResponse(FullHttpResponse fullHttpResponse) {
        FullHttpResponse response = new DefaultFullHttpResponse(fullHttpResponse.protocolVersion(), OK,
                Unpooled.wrappedBuffer(getByteBuf(fullHttpResponse)));
        response.headers()
                .set(CONTENT_TYPE, APPLICATION_JSON)
                .setInt(CONTENT_LENGTH, response.content().readableBytes());

        boolean keepAlive = HttpUtil.isKeepAlive(fullHttpResponse);
        if (keepAlive) {
            if (!fullHttpResponse.protocolVersion().isKeepAliveDefault()) {
                response.headers().set(CONNECTION, KEEP_ALIVE);
            }
        } else {
            // Tell the client we're going to close the connection.
            response.headers().set(CONNECTION, CLOSE);
        }
        return response;
    }

    private static byte[] getByteBuf(FullHttpResponse msg) {
        if (msg != null) {
            return msg.content().toString(CharsetUtil.UTF_8).getBytes();
        }
        return new byte[0];
    }

    public static FullHttpResponse creat404(HttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), NOT_FOUND,
                Unpooled.EMPTY_BUFFER);
        response.headers()
                .set(CONTENT_TYPE, APPLICATION_JSON)
                .setInt(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(CONNECTION, CLOSE);
        return response;
    }

    public static void responseNotFound(ChannelHandlerContext ctx, HttpRequest request) {
        FullHttpResponse fullHttpResponse = ResponseUtils.creat404(request);
        ctx.writeAndFlush(fullHttpResponse.retain()).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                ChannelUtils.closeOnFlush(ctx.channel());
            }
        });
    }
}
