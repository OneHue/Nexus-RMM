
package com.nexus.server.security;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PasswordSecurityTest {

    @Autowired
    private PasswordEncoder encoder;

    @Test
    void hashesPasswordsUsingArgon2id() {

        String password = "Example-Test-Passphrase-123!";

        String hash = encoder.encode(password);

        // The hash identifies the algorithm.
        assertTrue(hash.startsWith("{argon2id}$argon2id$"));

        // The original password is not stored.
        assertNotEquals(password, hash);
        assertFalse(hash.contains(password));

        // The correct password verifies.
        assertTrue(encoder.matches(password, hash));

        // An incorrect password does not.
        assertFalse(
                encoder.matches("Incorrect-Password", hash)
        );
    }

    @Test
    void identicalPasswordsProduceDifferentHashes() {

        String password = "Another-Example-Passphrase!";

        String firstHash = encoder.encode(password);
        String secondHash = encoder.encode(password);

        // Random salts produce different results.
        assertNotEquals(firstHash, secondHash);

        // Both hashes still verify successfully.
        assertTrue(encoder.matches(password, firstHash));
        assertTrue(encoder.matches(password, secondHash));
    }
}
