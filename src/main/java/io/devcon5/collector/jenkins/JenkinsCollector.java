package io.devcon5.collector.jenkins;

import io.devcon5.collector.AbstractCollector;
import io.devcon5.collector.ServiceClient;
import io.devcon5.measure.Measurement;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;

import java.util.ArrayList;
import java.util.List;

public class JenkinsCollector extends AbstractCollector {


    @Override
    protected void collectData(ServiceClient client, Handler<AsyncResult<Measurement[]>> resultHandler) {

        client.newGetRequest("/overallLoad/api/json?depth=2")
              .send(result -> resultHandler.handle(result.map(HttpResponse::bodyAsJsonObject)
                                                         .map(this::mapLoadStats)));
    }

    private Measurement[] mapLoadStats(JsonObject body) {
        return new Measurement[]{Measurement.builder()
                                            .name("loadStats")
                                            .value("availableExecutors", getFloatValue(body, "availableExecutors"))
                                            .value("busyExecutors", getFloatValue(body, "busyExecutors"))
                                            .value("connectingExecutors", getFloatValue(body, "connectingExecutors"))
                                            .value("definedExecutors", getFloatValue(body, "definedExecutors"))
                                            .value("idleExecutors", getFloatValue(body, "idleExecutors"))
                                            .value("onlineExecutors", getFloatValue(body, "onlineExecutors"))
                                            .value("queueLength", getFloatValue(body, "queueLength"))
                                            .value("totalExecutors", getFloatValue(body, "totalExecutors"))
                                            .value("totalQueueLength", getFloatValue(body, "totalQueueLength"))
                .build()};
    }

    private Float getFloatValue(JsonObject body, String paramName) {
        return body.getJsonObject(paramName).getJsonObject("min").getFloat("latest");
    }

}
