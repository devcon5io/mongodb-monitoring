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
