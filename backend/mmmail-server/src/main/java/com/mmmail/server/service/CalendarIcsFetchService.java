package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Service
public class CalendarIcsFetchService {

    private static final int FETCH_TIMEOUT_SECONDS = 5;
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 299;
    private static final String ACCEPT_ICS = "text/calendar,text/plain,*/*";

    private final HttpClient httpClient;

    public CalendarIcsFetchService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(FETCH_TIMEOUT_SECONDS))
                .build();
    }

    public URI validateHttpUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Calendar subscription URL must be HTTP or HTTPS");
            }
            return uri;
        } catch (URISyntaxException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Calendar subscription URL is invalid");
        }
    }

    public String fetch(String url, String authMode) {
        if (!"none".equals(authMode)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Calendar subscription auth mode is unsupported");
        }
        HttpRequest request = buildRequest(validateHttpUrl(url));
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < HTTP_SUCCESS_MIN || response.statusCode() > HTTP_SUCCESS_MAX) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Calendar subscription returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Failed to fetch calendar subscription");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Calendar subscription fetch was interrupted");
        }
    }

    private HttpRequest buildRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(FETCH_TIMEOUT_SECONDS))
                .header("Accept", ACCEPT_ICS)
                .GET()
                .build();
    }
}
