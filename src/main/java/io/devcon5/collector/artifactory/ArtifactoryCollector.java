package io.devcon5.collector.artifactory;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.devcon5.measure.BinaryEncoding;
import io.devcon5.measure.Digester;
import io.devcon5.measure.Encoder;
import io.devcon5.measure.Measurement;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

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
    private Encoder encoder;

    @Override
    public void start() throws Exception {

        //TODO support multiple servers
        final JsonObject config = config().getJsonArray("servers").getJsonObject(0);

        this.artifactoryHost = config.getString("host", "localhost");
        this.contextRoot = config.getString("contextRoot", "/artifactory/");
        this.artifactoryPort = config.getInteger("port", 8081);
        this.authorization = config.getString("auth", defaultBasicAuth());
        final long interval = config.getLong("interval", 60000L);

        this.webclient = WebClient.create(vertx);
        this.encoder = BinaryEncoding.encoder();

        vertx.setPeriodic(interval, this::pollStatus);
    }

    private String defaultBasicAuth() {

        return "Basic " + Base64.getEncoder()
                                .encodeToString("admin:password".getBytes());
    }

    private void pollStatus(long timerId) {

        HttpRequest<Buffer> request = webclient.get(artifactoryPort,
                                                    artifactoryHost,
                                                    contextRoot + "ui/storagesummary");
        if (this.authorization != null) {
            request.putHeader("Authorization", authorization);
        }
        request.send(this::processResult);
    }

    private void processResult(AsyncResult<HttpResponse<Buffer>> resp) {

        if (resp.succeeded()) {
            Measurement m = processStatistics(resp.result().bodyAsJsonObject());
            vertx.eventBus().publish(Digester.DIGEST_ADDR, encoder.encode(m));
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
