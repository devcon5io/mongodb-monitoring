/*
 * Copyright (C) 2017 DevCon5 GmbH
 */

package io.devcon5.collector;

import static org.slf4j.LoggerFactory.getLogger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

/**
 * Java launcher for the monitoring service. This launcher starts the Vert.x event loop and deploys the
 * verticles as configured. The verticle can either be launched using Vert.x CLI or directly via main method.
 */
public class Collector extends AbstractVerticle {

    private static final Logger LOG = getLogger(Collector.class);
    /**
     * Bootstrap for the collector.
     * @param args
     *  first argument must be the path to the config file
     */
    public static void main(String... args)  {

        if (args.length == 0) {
            System.out.println("usage: Collector [configFile]");
            return;
        }

        final Vertx vertx = Vertx.vertx();

        vertx.fileSystem().readFile(args[0], res -> {
            if (res.succeeded()) {
                vertx.deployVerticle(Collector.class, new DeploymentOptions().setConfig(res.result().toJsonObject()));
            } else {
                vertx.close();
            }
        });
    }


    @Override
    public void start() throws Exception {

        JsonObject config = config();

        config.getJsonObject("collector").forEach(e -> deployComponent(e.getKey(), (JsonObject)e.getValue()));
        config.getJsonObject("digester").forEach(e -> deployComponent(e.getKey(), (JsonObject)e.getValue()));
    }

    private void deployComponent(final String name, final JsonObject config) {

        String type = config.getString("type");

        LOG.info("Deploying {} of type {}", name, type);

        vertx.deployVerticle(type, new DeploymentOptions().setConfig(config));
    }

}
