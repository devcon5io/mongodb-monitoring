package io.devcon5.collector;

import io.devcon5.timeseries.Measurement;
import io.devcon5.timeseries.Digester;
import io.devcon5.units.SpaceUnit;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collector that periodically polls the artifactory server to fetch current storage statistics.
 * <p>
 * To configure the collector, use the following configuration:<br>
 * <pre><code>
 * {
 *   "host" : "yourArtifactoryHost",
 *   "port" : 80,
 *   "auth" : "Basic xxx",
 *   "interval" : 10000
 * }
 * </code></pre>
 * <p>
 * <ul>
 * <li>hostname - the hostname without any protocol, default is localhost</li>
 * <li>port - the port where artifactory accepts http requests, default is 80</li>
 * <li>auth - the raw Authorization header, for example to use Basic auth, set "Basic xxx" where xxx is
 * your Base64 encoded username:password</li>
 * <li>interval - the duration between each polls in ms, default is 60000 ms</li>
 * <li>contextRoot - the context root of the artifactor, default is / </li>
 * </ul>
 */
public class ArtifactoryCollector extends AbstractVerticle {

    private static final Pattern SPACE_PATTERN = Pattern.compile("(\\d+(\\.\\d+))?\\s(KB|MB|GB|TB)");
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s*%");
    private String artifactoryHost;
    private String contextRoot;
    private int artifactoryPort;
    private String authorization;

    private WebClient webclient;

    @Override
    public void start() throws Exception {

        final JsonObject config = config();

        this.artifactoryHost = config.getString("host", "localhost");
        this.contextRoot = config.getString("contextRoot", "/");
        this.artifactoryPort = config.getInteger("port", 80);
        this.authorization = config.getString("auth");
        final long interval = config.getLong("interval", 60000L);

        this.webclient = WebClient.create(vertx);

        vertx.setPeriodic(interval, this::pollStatus);
    }

    private void pollStatus(long timerId) {
        webclient.get(artifactoryPort, artifactoryHost, contextRoot + "ui/storagesummary")
                 .putHeader("Authorization", authorization)
                 .send(this::processResult);
    }

    private void processResult(AsyncResult<HttpResponse<Buffer>> resp) {
        if (resp.succeeded()) {
            Measurement m = processStatistics(resp.result().bodyAsJsonObject());
            vertx.eventBus().publish(Digester.DIGEST, m.toBuffer());
        } else {
            resp.cause().printStackTrace();
        }
    }


    private Measurement processStatistics(JsonObject obj) {
        JsonObject fsSummary = obj.getJsonObject("fileStoreSummary");

        return Measurement.builder()
                          .name("fileStorage")
                          .tag("server", "artifactory")
                          .value("totalSpace", parseSpace(fsSummary.getString("totalSpace")))
                          .value("usedSpace", parseSpace(fsSummary.getString("usedSpace")))
                          .value("usedSpacePercent", parseSpacePercent(fsSummary.getString("usedSpace")))
                          .value("freeSpace", parseSpace(fsSummary.getString("freeSpace")))
                          .value("freeSpacePercent", parseSpacePercent(fsSummary.getString("freeSpace")))
                          .build();
    }

    private Double parseSpacePercent(String space) {
        Matcher m = PERCENT_PATTERN.matcher(space);
        if (m.find()) {
            return Double.parseDouble(m.group(1)) / 100D;
        }
        return 0D;
    }

    private long parseSpace(String space) {
        SpaceUnit unit = SpaceUnit.parse(space);
        Matcher m = SPACE_PATTERN.matcher(space);
        if (m.find()) {
            return unit.toBytes(Double.parseDouble(m.group(1)));
        }
        return 0L;
    }

}