package io.devcon5.timeseries.influx;

import io.devcon5.timeseries.Measurement;
import io.devcon5.timeseries.Digester;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class InfluxDigester extends AbstractVerticle implements Digester {

    private InfluxClient client;

    @Override
    public void start() throws Exception {

        final JsonObject config = config();
        final String host = config.getString("host", "localhost");
        final int port = config.getInteger("port", 8086);
        final String db = config.getString("db");

        this.client = InfluxClient.create(vertx, host, port).useDatabase(db);

        vertx.eventBus().consumer(DIGEST, msg -> {
            final Measurement m = Measurement.fromBuffer((Buffer)msg.body());
            client.send(m, done -> {
                if(done.succeeded()){
                    System.out.println("Stored " + m);
                } else {
                    done.cause().printStackTrace();
                }
            });
        });
    }
}
