/*
 * Copyright (C) 2017 DevCon5 GmbH
 */

package mongodb.monitoring;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Java launcher for the monitoring service. This launcher starts the Vert.x event loop and deploys the
 * MongoDB monitoring verticle. This will periodically poll the mongoDB server for its status and pushes the data
 * into a timeseries DB.
 */
public class Launcher {

    public static void main(String... args) throws ExecutionException, InterruptedException {

        if(args.length == 0){
            System.out.println("usage: Launcher [configFile]");
            return;
        }
        String configFile = args[0];

        final Vertx vertx = Vertx.vertx();

        final JsonObject config = readFileToJson(vertx, configFile);

        vertx.deployVerticle("js:ServerStatusVerticle.js",
                new DeploymentOptions().setConfig(config));
    }

    /**
     * Reads a JSON file from the filesystem into a json object
     * @param vertx
     * @param configFile
     * @return
     */
    private static JsonObject readFileToJson(Vertx vertx, String configFile)
            throws ExecutionException, InterruptedException {
        final CompletableFuture<JsonObject> result = new CompletableFuture<>();
        vertx.fileSystem().readFile(configFile, res ->{
            if(res.succeeded()){
                result.complete(res.result().toJsonObject());
            } else {
                throw new RuntimeException("Reading Config File failed", res.cause());
            }
        });
        return result.get();
    }

}
