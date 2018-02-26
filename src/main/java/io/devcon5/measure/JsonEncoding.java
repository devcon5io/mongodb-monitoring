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

package io.devcon5.measure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 *
 */
public class JsonEncoding {

    public static Decoder<JsonArray> decoder() {
        return new JsonDecoder();
    }
    public static Encoder<JsonArray> encoder() {
        return new JsonEncoder();
    }

    private static class JsonEncoder implements Encoder<JsonArray>{

        @Override
        public JsonArray encode(final Collection<Measurement> m) {

            return new JsonArray(io.vertx.core.json.Json.encode(m));
        }
    }

    private static class JsonDecoder implements Decoder<JsonArray> {

        @Override
        public Measurement[] decode(final JsonArray encodedMeasurement) {

            List<Measurement> measurements = new ArrayList<>();

            for(int i = 0; i < encodedMeasurement.size(); i++){
                Object value = encodedMeasurement.getValue(i);
                if(!(value instanceof JsonObject)) {
                    throw new IllegalArgumentException("Invalid array structure, Object expected");
                }
                JsonObject jMeasurement = encodedMeasurement.getJsonObject(i);
                measurements.add(decode(jMeasurement));

            }
            return measurements.toArray(new Measurement[0]);
        }

        private Measurement decode(final JsonObject jsonObject) {

            Measurement.Builder builder = Measurement.builder();

            builder.name(jsonObject.getString("name"));
            builder.timestamp(jsonObject.getLong("timestamp"));

            JsonObject tags = jsonObject.getJsonObject("tags");
            tags.forEach(e -> builder.tag(e.getKey(), String.valueOf(e.getValue())));

            JsonObject value = jsonObject.getJsonObject("values");
            value.forEach(e -> builder.value(e.getKey(), e.getValue()));

            return builder.build();
        }
    }
}
