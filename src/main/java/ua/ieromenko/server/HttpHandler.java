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
        ConnectionLogUnit logUnit = new ConnectionLogUnit(requestIP, new Date());
        if (httpRequest != null) {
            String URI = httpRequest.getUri();

            //let`s handle this request
            UriHandler handler;
            if (URI.equals("/hello")) {
                handler = HELLO_URI_HANDLER;
            } else if (URI.matches("/redirect\\?url=\\S*")) {
                handler = REDIRECT_URI_HANDLER;
            } else if (URI.equals("/status")) {
                handler = STATUS_URI_HANDLER;
            } else handler = NOT_FOUND_URI_HANDLER;

            //send response
            FullHttpResponse response = handler.process(httpRequest);
            //close the connection immediately because no more requests can be sent from the browser
            response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);

            // do some statistics
            logUnit.setURI(URI); //
            ByteBuf buffer = Unpooled.copiedBuffer(httpRequest.toString().getBytes());
            int receivedBytes = buffer.readableBytes() + httpRequest.content().readableBytes();
            int sentBytes = response.content().writerIndex();
            logUnit.setReceivedBytes(receivedBytes);
            logUnit.setSentBytes(sentBytes);

            long time0 = System.nanoTime() - time;
            double time1 = time0 / (double) 1000000000;
            long speed = Math.round((sentBytes + receivedBytes) / time1);
            logUnit.setSpeed(speed);

            StatisticsHandler.addLogUnit(logUnit);
        }
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
