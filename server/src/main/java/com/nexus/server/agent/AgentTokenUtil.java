
package com.nexus.server.agent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class AgentTokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private AgentTokenUtil() {}

    public static String generate(String prefix) {

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        return prefix + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public static byte[] hash(String value) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return digest.digest(value.getBytes(StandardCharsets.UTF_8));

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    // NOTE: nxe_ = Nexus enrollment code; nxa_ = Nexus agent credential
}
