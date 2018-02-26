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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
