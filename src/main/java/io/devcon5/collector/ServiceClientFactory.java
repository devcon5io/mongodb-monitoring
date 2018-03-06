package io.devcon5.collector;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class ServiceClientFactory {

    private final WebClient client;

    protected ServiceClientFactory(WebClient client) {
        this.client = client;
    }

    public ServiceClient createClient(String host, int port, String contextRoot) {
        return new ServiceClient(client, host, port, contextRoot, null);
    }

    public ServiceClient createClient(String host, int port, String contextRoot, String auth) {
        return new ServiceClient(client, host, port, contextRoot, auth);
    }

    public ServiceClient createClient(JsonObject config) {
        return createClient(config.getString("host", "localhost"),
                            config.getInteger("port", 80),
                            config.getString("contextRoot", "/"),
                            config.getString("auth"));
    }

    public static ServiceClientFactory newFactory(final WebClient client) {
        return new ServiceClientFactory(client);
    }


}
