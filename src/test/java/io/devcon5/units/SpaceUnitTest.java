package io.devcon5.units;

import org.junit.Test;

import static org.junit.Assert.*;

public class SpaceUnitTest {

    @Test
    public void bytes_toBytes() {
        long bytes = SpaceUnit.B.toBytes(123);
        assertEquals(123, bytes);
    }
    @Test
    public void kbytes_toBytes() {
        long bytes = SpaceUnit.KB.toBytes(123);
        assertEquals(123 * 1024, bytes);
    }

    @Test
    public void mbytes_toBytes() {
        long bytes = SpaceUnit.MB.toBytes(12.3);
        assertEquals((long)(12.3 * 1024 * 1024), bytes);
    }

    @Test
    public void gbytes_toBytes() {
        long bytes = SpaceUnit.GB.toBytes(12.3);
        assertEquals((long)(12.3 * 1024 * 1024 * 1024), bytes);
    }

    @Test
    public void parse_unknown_toByte() {
        SpaceUnit unit = SpaceUnit.parse("1234 xx");
        assertEquals(SpaceUnit.B, unit);
    }

    @Test
    public void parse_B() {
        SpaceUnit unit = SpaceUnit.parse("1234 B");
        assertEquals(SpaceUnit.B, unit);
    }

    @Test
    public void parse_KB() {
        SpaceUnit unit = SpaceUnit.parse("12.34004 KB");
        assertEquals(SpaceUnit.KB, unit);
    }
    @Test
    public void parse_MB() {
        SpaceUnit unit = SpaceUnit.parse("123.4 MB");
        assertEquals(SpaceUnit.MB, unit);
    }
    @Test
    public void parse_GB() {
        SpaceUnit unit = SpaceUnit.parse("1234 GB");
        assertEquals(SpaceUnit.GB, unit);
    }
}