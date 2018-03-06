package io.devcon5.collector.sonarqube;

import io.devcon5.collector.ServiceClient;
import io.devcon5.collector.ServiceClientFactory;
import io.devcon5.measure.BinaryEncoding;
import io.devcon5.measure.Digester;
import io.devcon5.measure.Encoder;
import io.devcon5.measure.Measurement;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Override
    public void stop() throws Exception {
        this.webclient.close();
    }

    private void pollStatus(Long timer) {
        //projects
        client.newGetRequest("/api/components/search?qualifiers=BRC,DIR,FIL,TRK,UTS&ps=100000")
              .send(this::processProjects);
    }


    private void processProjects(AsyncResult<HttpResponse<Buffer>> resp) {

        if (resp.succeeded()) {

            JsonObject body = resp.result().bodyAsJsonObject();
            Measurement m = body.getJsonArray("components")
                                .stream()
                                .map(JsonObject.class::cast)
                                .map(o -> o.getString("qualifier"))
                                .map(this::mapComponentQualifier)
                                .collect(Collectors.groupingBy(Function.identity(),
                                        Collectors.reducing(0, o -> 1, Integer::sum)))
                                .entrySet()
                                .stream()
                                .collect(() -> Measurement.builder().name("components"),
                                        (b, e) -> b.value(e.getKey(), e.getValue()),
                                        (b1, b2) -> {}).build();

            vertx.eventBus()
                 .publish(Digester.DIGEST_ADDR, encoder.encode(m));
        } else {
            resp.cause().printStackTrace();
        }
    }

    private String mapComponentQualifier(String qualifier) {
        switch (qualifier) {
            case "TRK":
                return "projects";
            case "BRC":
                return "branches";
            case "DIR":
                return "directories";
            case "FIL":
                return "files";
            case "UTS":
                return "tests";
            default:
                return "unknown";
        }
    }
}
