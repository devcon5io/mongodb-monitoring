package io.devcon5.collector.sonarqube;

import org.junit.Test;

import static org.junit.Assert.*;

public class DurationParserTest {

    @Test
    public void parse_1min() {

        long val = DurationParser.parse("1min");
        assertEquals(1, val);
    }

    @Test
    public void parse_5min() {

        long val = DurationParser.parse("5min");
        assertEquals(5, val);
    }

    @Test
    public void parse_12min() {

        long val = DurationParser.parse("12min");
        assertEquals(12, val);
    }
}