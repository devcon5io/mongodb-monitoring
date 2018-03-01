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

package io.devcon5.collector.artifactory;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.devcon5.measure.BinaryEncoding;
import io.devcon5.measure.Digester;
import io.devcon5.measure.Encoder;
import io.devcon5.measure.Measurement;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

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

    private static final Logger LOG = getLogger(ArtifactoryCollector.class);

    private static final Pattern SPACE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s(bytes|B|KB|MB|GB|TB)");
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
                contextRoot + "api/storagesummary");
        if (this.authorization != null) {
            request.putHeader("Authorization", authorization);
        }
        request.send(this::processResult);
    }

    private void processResult(AsyncResult<HttpResponse<Buffer>> resp) {

        if (resp.succeeded()) {
            Collection<Measurement> m = processStatistics(resp.result().bodyAsJsonObject());
            vertx.eventBus().publish(Digester.DIGEST_ADDR, encoder.encode(m));
        } else {
            resp.cause().printStackTrace();
        }
    }

    private Collection<Measurement> processStatistics(JsonObject obj) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing statistics {}", obj.encodePrettily());
        }
        final List<Measurement> ms = new ArrayList<>();

        mapFileStorageStats(obj.getJsonObject("fileStoreSummary"), ms::add);
        mapBinaryStats(obj.getJsonObject("binariesSummary"), ms::add);
        mapRepositoryStats(obj.getJsonArray("repositoriesSummaryList"), ms::add);

        return ms;
    }

    private void mapFileStorageStats(JsonObject jsonObject, Consumer<Measurement> handler) {
        Optional.ofNullable(jsonObject).map(this::createFileStorageStats).ifPresent(handler);
    }

    private void mapBinaryStats(JsonObject jsonObject, Consumer<Measurement> handler) {
        Optional.ofNullable(jsonObject).map(this::createBinaryStats).ifPresent(handler);
    }

    private void mapRepositoryStats(JsonArray jsonArray, Consumer<Measurement> handler) {
        if (jsonArray == null) {
            return;
        }
        jsonArray.stream().map(JsonObject.class::cast)
                 .map(this::mapRepositoryStats).forEach(handler);

    }

    private Measurement createFileStorageStats(JsonObject json) {
        Measurement.Builder mb = Measurement.builder()
                                            .name("fileStorage")
                                            .tag("server", "artifactory");

        parseSpace(json, "totalSpace", mb::value);
        parseSpace(json, "usedSpace", mb::value);
        parseSpace(json, "freeSpace", mb::value);

        mb.value("freeSpacePercent", parsePercent(json.getString("freeSpace")));
        mb.value("usedSpacePercent", parsePercent(json.getString("usedSpace")));

        return mb.build();
    }

    private Measurement createBinaryStats(JsonObject json) {
        Measurement.Builder mb = Measurement.builder()
                                            .name("binaries")
                                            .tag("server", "artifactory");
        parseInteger(json, "binariesCount", mb::value);
        parseInteger(json, "itemsCount", mb::value);
        parseInteger(json, "artifactsCount", mb::value);

        parseSpace(json, "binariesSize", mb::value);
        parseSpace(json, "artifactsSize", mb::value);

        parsePercent(json, "optimization", mb::value);

        return mb.build();
    }

    private Measurement mapRepositoryStats(JsonObject json) {
        Measurement.Builder mb = Measurement.builder()
                                            .name("repositories")
                                            .tag("server", "artifactory")
                                            .tag("repository", json.getString("repoKey"))
                                            .tag("repoType", json.getString("repoType"))
                                            .tag("packageType", json.getString("packageType"))
                                            .value("itemsCount", json.getInteger("itemsCount", 0))
                                            .value("filesCount", json.getInteger("filesCount", 0))
                                            .value("foldersCount", json.getInteger("foldersCount", 0));

        parseSpace(json, "usedSpace", mb::value);

        parsePercent(json, "percentage", mb::value);


        return mb.build();
    }

    private void parseInteger(JsonObject obj, String property, BiConsumer<String, Integer> builder) {
        Optional.ofNullable(obj.getString(property))
                .map(this::parseInteger)
                .ifPresent(v -> builder.accept(property, v));

    }

    private void parsePercent(JsonObject obj, String property, BiConsumer<String, Double> builder) {
        Optional.ofNullable(obj.getString(property))
                .map(this::parsePercent)
                .ifPresent(v -> builder.accept(property, v));


    }

    private void parseSpace(JsonObject obj, String property, BiConsumer<String, Long> builder) {
        Optional.ofNullable(obj.getString(property))
                .map(this::parseSpace)
                .ifPresent(v -> builder.accept(property, v));
    }

    private Integer parseInteger(String rawValue) {
        try {
            return NumberFormat.getNumberInstance(Locale.getDefault())
                               .parse(rawValue)
                               .intValue();
        } catch (ParseException e) {
            LOG.warn("Could not parse {}", rawValue);
        }
        return 0;
    }


    private Double parsePercent(String space) {

        final Matcher m = PERCENT_PATTERN.matcher(space);
        if (m.find()) {
            return Double.parseDouble(m.group(1)) / 100D;
        }
        LOG.warn("Could not parse {}", space);
        return 0D;
    }

    private Long parseSpace(String space) {

        final Matcher m = SPACE_PATTERN.matcher(space);
        if (m.find()) {
            final String rawValue = m.group(1);
            try {
                return SpaceUnit.parse(space).toBytes(NumberFormat.getNumberInstance(Locale.getDefault())
                                                                  .parse(rawValue)
                                                                  .doubleValue());
            } catch (Exception e) {
                LOG.warn("Could not parse {}", rawValue);
            }
        }
        LOG.warn("Could not parse {}", space);
        return 0L;
    }

}
