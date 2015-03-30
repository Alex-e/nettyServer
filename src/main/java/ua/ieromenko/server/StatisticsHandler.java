package ua.ieromenko.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.AttributeKey;
import ua.ieromenko.util.ConnectionLogUnit;
import ua.ieromenko.util.LoggingQueue;
import ua.ieromenko.util.RequestsCounter;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.channel.ChannelHandler.Sharable;

/**
 * @Author Alexandr Ieromenko on 05.03.15.
 * <p/>
 * Statistics Handler
 */
@Sharable
public class StatisticsHandler extends ChannelTrafficShapingHandler {

    private static final AtomicInteger totalConnectionsCounter = new AtomicInteger(0);
    private static final AtomicInteger activeConnectionsCounter = new AtomicInteger(0);

    private static final ConcurrentHashMap<String, RequestsCounter> requestsCounter = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> redirectionPerURL = new ConcurrentHashMap<>();

    private static final LoggingQueue<ConnectionLogUnit> log = new LoggingQueue<>();

    public StatisticsHandler(long checkInterval) {
        super(checkInterval);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            totalConnectionsCounter.getAndIncrement();
            activeConnectionsCounter.getAndIncrement();

            HttpRequest request = (HttpRequest) msg;
            String URI = request.getUri();

            //IP REQUESTS COUNTER
            //UNIQUE REQUESTS PER IP COUNTER
            String requestIP = (((InetSocketAddress) ctx.channel().remoteAddress()).getHostString());
            RequestsCounter c;
            synchronized (requestsCounter) {
                if (!requestsCounter.containsKey(requestIP)) {
                    c = new RequestsCounter(requestIP, URI);
                    requestsCounter.put(requestIP, c);
                } else {
                    c = requestsCounter.get(requestIP).addRequest(URI);
                    requestsCounter.put(requestIP, c);
                }
            }


        }
        super.channelRead(ctx, msg);
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        activeConnectionsCounter.getAndDecrement();
        super.handlerRemoved(ctx);
    }

    public static void addLogUnit(ConnectionLogUnit unit1) {
            if (unit1 != null) log.add(unit1);
    }

    public static void addURLRedirection(String url) {
        synchronized (redirectionPerURL) {
            if (!redirectionPerURL.containsKey(url)) {
                redirectionPerURL.put(url, 1);
            } else {
                redirectionPerURL.put(url, redirectionPerURL.get(url) + 1);
            }
        }
    }


    public static int getTotalConnectionsCounter() {
        return totalConnectionsCounter.get();
    }

    public static int getActiveConnectionsCounter() {
        return activeConnectionsCounter.get();
    }

    public static ConcurrentHashMap<String, RequestsCounter> getRequestsCounter() {
        return requestsCounter;
    }

    public static ConcurrentHashMap<String, Integer> getRedirectionPerURL() {
        return redirectionPerURL;
    }

    public static LoggingQueue<ConnectionLogUnit> getLog() {
        return log;
    }
}
