package io.devcon5.collector.mongo;

import io.devcon5.Docker;
import io.devcon5.measure.Decoder;
import io.devcon5.measure.Digester;
import io.devcon5.measure.JsonEncoding;
import io.devcon5.measure.Measurement;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.Wait;

/**
 *
 */
@RunWith(VertxUnitRunner.class)
public class MongoCollectorIT {

    @ClassRule
    public static GenericContainer mongo = Docker.run("mongo:latest")
                                                 .withExposedPorts(27017)
                                                 .waitingFor(Wait.forListeningPort());

    private Decoder<JsonArray> decoder = JsonEncoding.decoder();

    @Test
    public void should_fetch_and_publish_measurement(TestContext context) throws Exception {

        JsonObject config = new JsonObject().put("interval", 1000)
                                            .put("mongoServer",
                                                 new JsonArray().add(new JsonObject().put("host",
                                                                                          mongo.getContainerIpAddress())
                                                                                     .put("db_name", "test")
                                                                                     .put("port",
                                                                                          mongo.getMappedPort(27017))
                                                                                     .put("collections",
                                                                                          new JsonArray().add("example"))));

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle("js:io/devcon5/collector/mongo/MongoCollector.js",
                             new DeploymentOptions().setConfig(config));

        final Async measureReceived = context.async(3);
        vertx.eventBus().consumer(Digester.DIGEST_ADDR, msg -> {
            Object obj = msg.body();
            context.assertTrue(obj instanceof JsonArray);

            Measurement[] m = decoder.decode((JsonArray) obj);
            context.assertTrue(m.length > 0);

            measureReceived.countDown();
        });

    }

}
