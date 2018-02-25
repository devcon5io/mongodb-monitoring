package io.devcon5.collector.artifactory;

import java.util.Arrays;
import java.util.Base64;

import io.devcon5.Docker;
import io.devcon5.measure.BufferEncoding;
import io.devcon5.measure.Decoder;
import io.devcon5.measure.Digester;
import io.devcon5.measure.Measurement;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
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
public class ArtifactoryCollectorIT {

    @ClassRule
    public static GenericContainer artifactory = Docker.run("docker.bintray.io/jfrog/artifactory-oss:latest")
                                                       .withExposedPorts(8081)
                                                       .waitingFor(Wait.forHttp("/artifactory/api/application.wadl"));

    private Decoder<Buffer> decoder = BufferEncoding.decoder();

    @Test
    public void should_fetch_and_publish_measurement(TestContext context) throws Exception {

        JsonObject artifactoryConfig = new JsonObject().put("host", artifactory.getContainerIpAddress())
                                                       .put("port", artifactory.getMappedPort(8081))
                                                       .put("auth", defaultBasicAuth())
                                                       .put("interval", 1000L);

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(ArtifactoryCollector.class.getName(),
                             new DeploymentOptions().setConfig(artifactoryConfig));

        final Async measureReceived = context.async();

        vertx.eventBus().consumer(Digester.DIGEST_ADDR, msg -> {
            Measurement[] m = decoder.decode((Buffer) msg.body());
            System.out.println(Arrays.asList(m));

            context.assertEquals("fileStorage", m[0].getName());
            measureReceived.complete();
        });
    }

    private String defaultBasicAuth() {

        return "Basic " + Base64.getEncoder()
                                .encodeToString(("admin:password".getBytes()));
    }

}
