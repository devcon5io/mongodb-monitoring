package io.devcon5.collector.mongo;

import java.util.Arrays;

import io.devcon5.Docker;
import io.devcon5.measure.Decoder;
import io.devcon5.measure.Digester;
import io.devcon5.measure.JsonEncoding;
import io.devcon5.measure.Measurement;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
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
  public static GenericContainer mongo = Docker.run("mongo:latest").withExposedPorts(27017).waitingFor(Wait.forListeningPort());
  @Rule
  public RunTestOnContext testContext = new RunTestOnContext();
  private Decoder<JsonArray> decoder = JsonEncoding.decoder();

  @Before
  public void setUp(TestContext context) throws Exception {

    MongoClient client = MongoClient.createShared(testContext.vertx(),
                                                  new JsonObject().put("host", mongo.getContainerIpAddress())
                                                                  .put("port", mongo.getMappedPort(27017))
                                                                  .put("db_name", "test"));
    Async async = context.async();
    client.createCollection("example", result -> {
      async.complete();
      if(result.failed()){
        context.fail(result.cause());

      }
    });
  }

  @Test
  public void should_fetch_and_publish_measurement(TestContext context) throws Exception {

    JsonObject config = new JsonObject().put("interval", 1000)
                                        .put("servers",
                                             new JsonArray().add(new JsonObject().put("host", mongo.getContainerIpAddress())
                                                                                 .put("db_name", "test")
                                                                                 .put("port", mongo.getMappedPort(27017))
                                                                                 .put("collections", new JsonArray().add("example"))));

    Vertx vertx = testContext.vertx();
    vertx.deployVerticle("js:io/devcon5/collector/mongo/MongoCollector.js", new DeploymentOptions().setConfig(config));

    final Async measureReceived = context.async(3);
    vertx.eventBus().consumer(Digester.DIGEST_ADDR, msg -> {
      Object obj = msg.body();
      context.assertTrue(obj instanceof JsonArray);

      Measurement[] m = decoder.decode((JsonArray) obj);
      System.out.println(Arrays.toString(m));
      context.assertTrue(m.length > 0);

      measureReceived.countDown();
    });

  }

}
