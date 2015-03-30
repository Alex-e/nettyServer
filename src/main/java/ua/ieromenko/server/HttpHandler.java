package ua.ieromenko.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import ua.ieromenko.UriHandlers.*;
import ua.ieromenko.util.ConnectionLogUnit;

import java.net.InetSocketAddress;
import java.util.Date;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

/**
 * @Author Alexandr Ieromenko on 04.03.15.
 * <p/>
 * Main HttpRequests handler
 * <p/>
 */
class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final NotFoundUriHandler NOT_FOUND_URI_HANDLER = new NotFoundUriHandler();
    private static final HelloUriHandler HELLO_URI_HANDLER = new HelloUriHandler();
    private static final RedirectUriHandler REDIRECT_URI_HANDLER = new RedirectUriHandler();
    private static final StatusUriHandler STATUS_URI_HANDLER = new StatusUriHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        long time = System.nanoTime();

        String requestIP = (((InetSocketAddress) ctx.channel().remoteAddress()).getHostString());
        String URI = httpRequest.getUri();

        FullHttpResponse response = writeResponse(URI);
            //close the connection immediately because no more requests can be sent from the browser
        ctx.write(response).addListener(ChannelFutureListener.CLOSE);

        // do some statistics
        ByteBuf buffer = Unpooled.copiedBuffer(httpRequest.toString().getBytes());
        int receivedBytes = buffer.readableBytes() + httpRequest.content().readableBytes();
        int sentBytes = response.content().writerIndex();
        long time0 = System.nanoTime() - time;
        double time1 = time0 / (double) 1000000000;
        long speed = Math.round((sentBytes + receivedBytes) / time1);

        ConnectionLogUnit logUnit = new ConnectionLogUnit(requestIP, URI, sentBytes, receivedBytes, speed);

        StatisticsHandler.addLogUnit(logUnit);
    }

    private FullHttpResponse writeResponse(String uri) {
        UriHandler handler;
        if (uri.equals("/hello")) {
            handler = HELLO_URI_HANDLER;
        } else if (uri.matches("/redirect\\?url=\\S*")) {
            handler = REDIRECT_URI_HANDLER;
        } else if (uri.equals("/status")) {
            handler = STATUS_URI_HANDLER;
        } else handler = NOT_FOUND_URI_HANDLER;
        return handler.process(uri);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
