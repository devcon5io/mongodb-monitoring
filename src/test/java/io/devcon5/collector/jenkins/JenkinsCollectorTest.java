package io.devcon5.collector.jenkins;

import io.devcon5.collector.Auth;
import io.devcon5.collector.sonarqube.SonarqubeAuth;
import io.devcon5.collector.sonarqube.SonarqubeCollector;
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

import static org.junit.Assert.*;

@RunWith(VertxUnitRunner.class)
public class JenkinsCollectorTest {

    @Rule
    public RunTestOnContext run = new RunTestOnContext();

    @Test
    public void collectData(TestContext context) throws Exception {

        final Vertx vertx = run.vertx();
        final Decoder<Buffer> decoder = BinaryEncoding.decoder();

        JsonObject config = new JsonObject()
                .put("interval", 1000)
                .put("servers", new JsonArray().add(new JsonObject()
                        .put("host", "localhost")
                        .put("port", 80)
                        .put("auth", Auth.basic("test", "test"))));

        vertx.deployVerticle(JenkinsCollector.class.getName(), new DeploymentOptions().setConfig(config));

        final Async measureReceived = context.async();

        vertx.eventBus().consumer(Digester.DIGEST_ADDR, msg -> {
            Measurement[] m = decoder.decode((Buffer) msg.body());
            System.out.println(Arrays.asList(m));
            measureReceived.complete();
        });
    }
}