package io.devcon5.measure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

/**
 *
 */
public class MeasurementTest {

    @Test
    public void getTimestamp() throws Exception {

        Measurement m = Measurement.builder().name("name").timestamp(123).value("test", 123).build();
        assertEquals(123, m.getTimestamp());
    }

    @Test
    public void getName() throws Exception {

        Measurement m = Measurement.builder().name("name").timestamp(123).value("test", 123).build();
        assertEquals("name", m.getName());
    }

    @Test
    public void getTags() throws Exception {

        Measurement m = Measurement.builder()
                                   .name("name")
                                   .timestamp(123)
                                   .tag("tag1", "t1")
                                   .tag("tag2", "t2")
                                   .value("test", 123)
                                   .build();

        Map<String, String> tags = m.getTags();
        assertEquals("t1", tags.get("tag1"));
        assertEquals("t2", tags.get("tag2"));

    }

    @Test(expected = UnsupportedOperationException.class)
    public void getTags_unmodifiable() throws Exception {

        Measurement m = Measurement.builder()
                                   .name("name")
                                   .timestamp(123)
                                   .tag("tag1", "t1")
                                   .tag("tag2", "t2")
                                   .value("test", 123)
                                   .build();

        m.getTags().clear();

    }

    @Test
    public void getValues() throws Exception {

        Measurement m = Measurement.builder()
                                   .name("name")
                                   .timestamp(123)
                                   .value("int", 123)
                                   .value("long", 123L)
                                   .value("float", 123.1F)
                                   .value("double", 123.1D)
                                   .value("boolean", true)
                                   .value("string", "123")
                                   .build();

        Map<String, Object> values = m.getValues();

        assertEquals(Integer.valueOf(123), values.get("int"));
        assertEquals(Long.valueOf(123L), values.get("long"));
        assertEquals(Float.valueOf(123.1F), values.get("float"));
        assertEquals(Double.valueOf(123.1D), values.get("double"));
        assertEquals(Boolean.TRUE, values.get("boolean"));
        assertEquals("123", values.get("string"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getValues_unmodifiable() throws Exception {

        Measurement m = Measurement.builder()
                                   .name("name")
                                   .timestamp(123)
                                   .value("int", 123)
                                   .value("long", 123L)
                                   .value("float", 123.1)
                                   .value("double", 123.1D)
                                   .value("boolean", true)
                                   .value("string", "123")
                                   .build();
        m.getValues().clear();
    }

    @Test
    public void test_toString() throws Exception {

        Measurement m = Measurement.builder()
                                   .name("name")
                                   .timestamp(123)
                                   .tag("tag1", "t1")
                                   .tag("tag2", "t2")
                                   .value("int", 123)
                                   .value("long", 123L)
                                   .value("float", 123.1F)
                                   .value("double", 123.1D)
                                   .value("boolean", true)
                                   .value("string", "123")
                                   .build();
        String s = m.toString();

        //values are sorted by name
        assertEquals(
                "Measurement{name='name', timestamp=123, tags={tag1=t1, tag2=t2}, values={boolean=true, double=123.1, float=123.1, int=123, long=123, string=123}}",
                s);
    }

    @Test
    public void builder() throws Exception {

        assertNotNull(Measurement.builder());
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_noName_exception() throws Exception {

        assertNotNull(Measurement.builder().timestamp(123).value("val", 123).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_emptyName_exception() throws Exception {

        assertNotNull(Measurement.builder().name("").timestamp(123).value("val", 123).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_negativeTimestamp_exception() throws Exception {

        assertNotNull(Measurement.builder().name("test").timestamp(-123).value("val", 123).build());
    }

    @Test
    public void builder_noTimeStamp_default() throws Exception {

        long now = System.currentTimeMillis();
        Measurement m = Measurement.builder().name("test").value("val", 123).build();
        assertTrue(m.getTimestamp() >= now * 1_000_000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_noValues_exception() throws Exception {

        assertNotNull(Measurement.builder().name("test").timestamp(-123).build());
    }

}
