package io.devcon5.collector.sonarqube;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile("([0-9]+)min",Pattern.CASE_INSENSITIVE);

    public static long parse(String input){

        Matcher matcher = DURATION_PATTERN.matcher(input);
        if(matcher.find()){
            return Long.parseLong(matcher.group(1));
        }
        return 0;
    }
}
