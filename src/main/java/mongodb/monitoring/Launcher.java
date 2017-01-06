package mongodb.monitoring;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 *
 */
public class Launcher {

    public static void main(String... args) {
        String configFile = "src/main/resources/test_config.json";

        final Vertx vertx = Vertx.vertx();
        final JsonObject config = readFileToJson(vertx, configFile);

        vertx.deployVerticle("js:mongodb/monitoring/ServerStatusVerticle.js",
                new DeploymentOptions().setConfig(config));
    }


    private static JsonObject readFileToJson(Vertx vertx, String configFile) {
        final JsonObject config = new JsonObject();
        vertx.fileSystem().readFile(configFile, result->{
            if(result.succeeded()){
                config.mergeIn(result.result().toJsonObject());
            } else {
                throw new RuntimeException("Reading Config File failed", result.cause());
            }
        });
        return config;
    }

}
