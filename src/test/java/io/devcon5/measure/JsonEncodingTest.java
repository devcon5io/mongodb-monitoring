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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.vertx.core.json.JsonArray;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class JsonEncodingTest {
    private Encoder<JsonArray> encoder;
    private Decoder<JsonArray> decoder;

    @Before
    public void setUp() throws Exception {
        this.encoder = JsonEncoding.encoder();
        this.decoder = JsonEncoding.decoder();
    }


    @Test
    public void encode_decode_single() throws Exception {

        //The jsonEncoding can not distinguish between float/double and int/long.
        // therefore the test data contains only double & int values

        Measurement m = Measurement.builder()
                                   .name("test")
                                   .timestamp(123456789)
                                   .tag("tag1", "t1")
                                   .tag("tag2", "t2")
                                   .value("int", 123)
                                   .value("double", 123.1D)
                                   .value("boolean", true)
                                   .value("string", "123")
                                   .build();

        JsonArray b = encoder.encode(m);

        assertNotNull(b);

        Measurement[] m2 = decoder.decode(b);

        assertEquals(m, m2[0]);

    }

    @Test
    public void encode_decode_multiple() throws Exception {

        //The jsonEncoding can not distinguish between float/double and int/long.
        // therefore the test data contains only double & int values

        Measurement m1 = Measurement.builder()
                                    .name("test")
                                    .timestamp(123456789)
                                    .tag("tag1", "t1")
                                    .tag("tag2", "t2")
                                    .value("int", 123)
                                    .value("double", 123.1D)
                                    .value("boolean", true)
                                    .value("string", "123")
                                    .build();
        Measurement m2 = Measurement.builder()
                                    .name("test2")
                                    .timestamp(123456789)
                                    .tag("tag1", "t3")
                                    .tag("tag2", "t4")
                                    .value("int", 456)
                                    .value("double", 456.1D)
                                    .value("boolean", false)
                                    .value("string", "456")
                                    .build();

        JsonArray b = encoder.encode(m1,m2);

        assertNotNull(b);

        Measurement[] ms = decoder.decode(b);

        assertEquals(m1, ms[0]);
        assertEquals(m2, ms[1]);

    }

    @Test(expected = IllegalArgumentException.class)
    public void decode_invalidEncoding() throws Exception {

        decoder.decode(new JsonArray().add("abc"));

    }

    @Test
    public void decode_emptyBuffer_emptyArray() throws Exception {

        Measurement[] ms = decoder.decode(new JsonArray());

        assertEquals(0, ms.length);

    }

}
