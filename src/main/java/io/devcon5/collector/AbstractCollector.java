package io.devcon5.collector;

import io.devcon5.collector.jenkins.JenkinsCollector;
import io.devcon5.measure.BinaryEncoding;
import io.devcon5.measure.Digester;
import io.devcon5.measure.Encoder;
import io.devcon5.measure.Measurement;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractCollector extends AbstractVerticle {

    private static final Logger LOG = getLogger(AbstractCollector.class);

    private Encoder<Buffer> encoder;
    private WebClient webclient;
    private ResultHandler<Measurement[]> resultHandler;

    @Override
    public void start() throws Exception {
        this.encoder = BinaryEncoding.encoder();
        this.webclient = WebClient.create(vertx);
        this.resultHandler = ResultHandler.create(Measurement[].class)
                                          .onSuccess(ms -> vertx.eventBus()
                                                                .publish(Digester.DIGEST_ADDR, encoder.encode(ms)))
                                          .orElse(t -> LOG.error("Error fetching metrics", t));

        //default interval for all servers
        final long interval = config().getLong("interval", 60000L);

        final ServiceClientFactory factory = ServiceClientFactory.newFactory(webclient);

        config().getJsonArray("servers")
                .stream()
                .map(JsonObject.class::cast)
                .forEach(config -> vertx.setPeriodic(config().getLong("interval", interval),
                        pollStatus(factory.createClient(config))));
    }

    @Override
    public void stop() {
        this.webclient.close();
    }

    private Handler<Long> pollStatus(ServiceClient client) {
        return l -> collectData(client, resultHandler::accept);

    }

    protected abstract void collectData(ServiceClient client, Handler<AsyncResult<Measurement[]>> resultHandler);
}
