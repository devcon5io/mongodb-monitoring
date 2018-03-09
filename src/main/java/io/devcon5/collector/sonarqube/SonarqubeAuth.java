package io.devcon5.collector.sonarqube;

import java.util.Base64;

public final class SonarqubeAuth {

    private SonarqubeAuth(){}

    public static String token(String token){
        return "Basic " + Base64.getEncoder().encodeToString((token + ":").getBytes());
    }

    public static String basic(String username, String password){
        return "Basic " + Base64.getEncoder()
                                .encodeToString("admin:password".getBytes());
    }
}

