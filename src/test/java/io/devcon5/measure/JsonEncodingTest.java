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
