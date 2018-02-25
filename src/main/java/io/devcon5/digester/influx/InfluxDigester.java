package io.devcon5.digester.influx;

import java.util.Arrays;

import io.devcon5.measure.Digester;
import io.devcon5.measure.Measurement;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class InfluxDigester extends AbstractVerticle implements Digester {

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
                if(done.succeeded()){
                    System.out.println("Stored " + Arrays.toString(m));
                } else {
                    done.cause().printStackTrace();
                }
            });
        });
    }
}
