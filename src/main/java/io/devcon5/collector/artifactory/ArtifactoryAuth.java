package io.devcon5.collector.artifactory;

import java.util.Base64;

import static io.devcon5.collector.Auth.basic;

public final class ArtifactoryAuth {

    private ArtifactoryAuth(){}

    public static String defaultBasicAuth() {
        return basic("admin", "password");
    }
}
