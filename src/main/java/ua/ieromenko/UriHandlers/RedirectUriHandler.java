package ua.ieromenko.UriHandlers;

/**
 * @Author Alexandr Ieromenko on 03.03.15.
 */

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import ua.ieromenko.server.StatisticsHandler;

import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class RedirectUriHandler implements UriHandler {

    @Override
    public FullHttpResponse process(HttpRequest request) {
        String requestURI = request.getUri();
        String url = requestURI.substring(requestURI.indexOf("url=") + 1, requestURI.length());

        StatisticsHandler.addURLRedirection(url);

        if (!url.matches("/redirect\\?url=http\\S*")) {
            url = "http://" + url;
        }
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, FOUND);
        response.headers().set(HttpHeaders.Names.LOCATION, url);
        return response;
    }
}
