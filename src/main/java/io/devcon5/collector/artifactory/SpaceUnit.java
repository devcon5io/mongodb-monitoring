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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum SpaceUnit {

    B(1),
    KB(1024),
    MB(1024*1024),
    GB(1024*1024*1024)
    ;

    private final long multiplier;
    private static final Pattern spaceUnitPattern = Pattern.compile("(KB|MB|GB|TB)");

    SpaceUnit(long multiplier){
        this.multiplier = multiplier;
    }

    public long toBytes(double base){
        return (long) (base * multiplier);
    }

    public static SpaceUnit parse(String spaceString){
        Matcher m = spaceUnitPattern.matcher(spaceString);
        if(m.find()){
            return valueOf(m.group(1));
        }
        return B;
    }
}
