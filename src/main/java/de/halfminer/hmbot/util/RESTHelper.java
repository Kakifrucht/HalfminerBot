package de.halfminer.hmbot.util;

import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * HTTP helper functions for communication with Halfminer REST API.
 */
public final class RESTHelper {

    private final static String baseUrl = "https://api.halfminer.de/";

    private RESTHelper() {}

    public static String getBaseUrl(String uri) {
        return baseUrl + uri;
    }

    public static RequestBody getRequestBody(String body) {
        return RequestBody.create(body, MediaType.parse("application/x-www-form-urlencoded"));
    }
}
