package io.devcon5.collector;

import java.util.Base64;

public final class Auth {
    private Auth(){}

    public static String basic(String username, String password){
        return "Basic " + Base64.getEncoder()
                                .encodeToString((username + ":" + password).getBytes());
    }
}
