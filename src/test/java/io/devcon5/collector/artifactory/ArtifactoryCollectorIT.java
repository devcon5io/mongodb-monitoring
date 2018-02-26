/*
 *     Universal Collector for Metrics
 *     Copyright (C) 2017-2018 DevCon5 GmbH, Switzerland
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.devcon5.collector.artifactory;

import java.util.Arrays;
import java.util.Base64;

import io.devcon5.Docker;
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

  private Decoder<Buffer> decoder = BinaryEncoding.decoder();

  @Test
  public void should_fetch_and_publish_measurement(TestContext context) throws Exception {

    JsonObject artifactoryConfig = new JsonObject().put("servers",
                                                        new JsonArray().add(new JsonObject().put("host", artifactory.getContainerIpAddress())
                                                                                            .put("port", artifactory.getMappedPort(8081))
                                                                                            .put("auth", defaultBasicAuth()))).put("interval", 1000L);

    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(ArtifactoryCollector.class.getName(), new DeploymentOptions().setConfig(artifactoryConfig));

    final Async measureReceived = context.async();

    vertx.eventBus().consumer(Digester.DIGEST_ADDR, msg -> {
      Measurement[] m = decoder.decode((Buffer) msg.body());
      System.out.println(Arrays.asList(m));

      context.assertEquals("fileStorage", m[0].getName());
      measureReceived.complete();
    });
  }

  private String defaultBasicAuth() {

    return "Basic " + Base64.getEncoder().encodeToString(("admin:password".getBytes()));
  }

}
