package io.devcon5.collector.sonarqube;

import io.devcon5.collector.ServiceClient;
import io.devcon5.collector.ServiceClientFactory;
import io.devcon5.measure.BinaryEncoding;
import io.devcon5.measure.Digester;
import io.devcon5.measure.Encoder;
import io.devcon5.measure.Measurement;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import java.util.Collection;
import java.util.Optional;

public class SonarqubeCollector extends AbstractVerticle {

    private WebClient webclient;
    private Encoder<Buffer> encoder;
    private ServiceClient client;

    @Override
    public void start() throws Exception {
        final JsonObject config = config().getJsonArray("servers").getJsonObject(0);

        this.encoder = BinaryEncoding.encoder();
        this.webclient = WebClient.create(vertx);

        final ServiceClientFactory factory = ServiceClientFactory.newFactory(webclient);

        //TODO support multiple servers
        this.client = factory.createClient(config);
        final long interval = config.getLong("interval", 60000L);

        vertx.setPeriodic(interval, this::pollStatus);
    }

    private void pollStatus(Long timer) {
        client.newGetRequest("/api/components/search?qualifiers=BRC").send(this::processResult);
    }

    private void processResult(AsyncResult<HttpResponse<Buffer>> resp) {

        if (resp.succeeded()) {

            System.out.println(resp.result().bodyAsJsonObject().encodePrettily());

//            vertx.eventBus().publish(Digester.DIGEST_ADDR, encoder.encode(m));
        } else {
            resp.cause().printStackTrace();
        }
    }

}
