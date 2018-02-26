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

        LOG.info("Deploying {} ({})", name, type);

        vertx.deployVerticle(type, new DeploymentOptions().setConfig(config));
    }

}
