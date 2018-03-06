package io.devcon5.collector.sonarqube;

import io.devcon5.measure.BinaryEncoding;
import io.devcon5.measure.Decoder;
import io.devcon5.measure.Digester;
import io.devcon5.measure.Measurement;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(VertxUnitRunner.class)
public class SonarqubeCollectorIT {

    @Rule
    public RunTestOnContext run = new RunTestOnContext();

    @Test
    public void testCollectorLocal(TestContext context) throws Exception {

        final Vertx vertx = run.vertx();
        final Decoder<Buffer> decoder = BinaryEncoding.decoder();

        JsonObject config = new JsonObject()
                .put("servers", new JsonArray().add(new JsonObject()
                        .put("host", "localhost")
                        .put("port", 80)
                        .put("auth", SonarqubeAuth.token("883b8127eec4a18f380be302fc2f1f0f07e8f539"))
                        .put("interval", 1000)));

        vertx.deployVerticle(SonarqubeCollector.class.getName(), new DeploymentOptions().setConfig(config));

        final Async measureReceived = context.async();

        vertx.eventBus().consumer(Digester.DIGEST_ADDR, msg -> {

            Measurement[] m = decoder.decode((Buffer) msg.body());
            System.out.println(Arrays.asList(m));
            measureReceived.complete();
        });


    }
}