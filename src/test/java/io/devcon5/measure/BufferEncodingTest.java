package io.devcon5.measure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.vertx.core.buffer.Buffer;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class BufferEncodingTest {

    private Encoder<Buffer> encoder;
    private Decoder<Buffer> decoder;

    @Before
    public void setUp() throws Exception {
        this.encoder = BufferEncoding.encoder();
        this.decoder = BufferEncoding.decoder();
    }


    @Test
    public void encode_decode() throws Exception {

        Measurement m = Measurement.builder()
                                   .name("test")
                                   .timestamp(123456789)
                                   .tag("tag1", "t1")
                                   .tag("tag2", "t2")
                                   .value("int", 123)
                                   .value("long", 123L)
                                   .value("float", 123.1)
                                   .value("double", 123.1D)
                                   .value("boolean", true)
                                   .value("string", "123")
                                   .build();

        Buffer b = encoder.encode(m);

        assertNotNull(b);

        Measurement m2 = decoder.decode(b);

        assertEquals(m, m2);

    }

    @Test(expected = IllegalArgumentException.class)
    public void decode_invalidEncoding() throws Exception {

        decoder.decode(Buffer.buffer("123"));

    }
}
