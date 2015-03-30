package ua.ieromenko.UriHandlers;

/**
 * @Author Alexandr Ieromenko on 03.03.15.
 */

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import ua.ieromenko.server.StatisticsHandler;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class RedirectUriHandler implements UriHandler {

    @Override
    public FullHttpResponse process(String uri) {
        String url = uri.substring(uri.indexOf("=") + 1, uri.length());

        StatisticsHandler.addURLRedirection(url);

        if (!url.matches("http://\\S*")) {
            url = "http://" + url;
        }

        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, FOUND);
        response.headers().set(HttpHeaders.Names.LOCATION, url);
        response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
        return response;
    }
}
