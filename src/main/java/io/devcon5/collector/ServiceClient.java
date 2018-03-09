package io.devcon5.collector;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;

import java.util.Optional;

/**
 * A generic client for a service running on a specific host, port and context root.
 * Clients are created using the {@link ServiceClientFactory}.
 * The client supports setting an authentication header
 */
public class ServiceClient {

    private final WebClient client;
    private final String host;
    private final int port;
    private final String contextRoot;
    private final Optional<String> auth;

    protected ServiceClient(WebClient client, String host, int port, String contextRoot, String auth) {
        this.client = client;
        this.host = host;
        this.port = port;
        this.contextRoot = contextRoot;
        this.auth = Optional.ofNullable(auth);
    }

    public HttpRequest<Buffer> newGetRequest(String url){
        HttpRequest<Buffer> request = client.get(port, host, createRequestURI(url));
        auth.ifPresent(t -> request.putHeader("Authorization", t));
        return request;
    }

    private String createRequestURI(String url) {

        return contextRoot + (url.startsWith("/") ? url.substring(1) : url);
    }
}
