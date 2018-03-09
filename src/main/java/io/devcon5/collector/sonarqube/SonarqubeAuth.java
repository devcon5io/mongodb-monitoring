package io.devcon5.collector.sonarqube;

import io.devcon5.collector.Auth;

public final class SonarqubeAuth {

    private SonarqubeAuth() {
    }

    public static String token(String token) {
        return Auth.basic(token, "");
    }
}

