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

package io.devcon5.digester.influx;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;

import io.devcon5.measure.Digester;
import io.devcon5.measure.Measurement;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

public class InfluxDigester extends AbstractVerticle implements Digester {

    private static final Logger LOG = getLogger(InfluxDigester.class);

    private InfluxClient client;

    @Override
    public void start() throws Exception {

        final JsonObject config = config();
        final String host = config.getString("host", "localhost");
        final int port = config.getInteger("port", 8086);
        final String db = config.getString("database");

        this.client = InfluxClient.create(vertx, host, port).useDatabase(db);
        vertx.eventBus().consumer(DIGEST_ADDR, msg -> {
            final Measurement[] m = decode(msg.body());
            client.send(Arrays.asList(m), done -> {
                if (done.succeeded()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Stored {}", Arrays.toString(m));
                    }
                } else {
                    done.cause().printStackTrace();
                }
            });
        });
    }
}
