package io.devcon5.collector.artifactory;

import java.util.Base64;

public final class ArtifactoryAuth {

    private ArtifactoryAuth(){}

    public static String basic(String username, String password){
        return "Basic " + Base64.getEncoder()
                                .encodeToString((username + ":" + password).getBytes());
    }

    public static String defaultBasicAuth() {
        return basic("admin", "password");
    }
}
