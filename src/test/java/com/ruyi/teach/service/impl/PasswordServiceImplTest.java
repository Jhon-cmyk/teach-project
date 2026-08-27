package com.ruyi.teach.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceImplTest {

    private PasswordServiceImpl passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordServiceImpl(new BCryptPasswordEncoder());
    }

    @Test
    void encodesNewPasswordsWithBcrypt() {
        String encoded = passwordService.encode("correct-password");

        assertNotEquals("correct-password", encoded);
        assertTrue(passwordService.matches("correct-password", encoded));
        assertFalse(passwordService.needsUpgrade(encoded));
    }

    @Test
    void matchesLegacyMd5AndRequestsUpgrade() {
        String legacyHash = legacyHash("old-password");

        assertTrue(passwordService.matches("old-password", legacyHash));
        assertTrue(passwordService.needsUpgrade(legacyHash));
    }

    @Test
    void rejectsWrongPasswordAndUnknownHashFormat() {
        String encoded = passwordService.encode("correct-password");

        assertFalse(passwordService.matches("wrong-password", encoded));
        assertFalse(passwordService.matches("correct-password", "not-a-supported-hash"));
        assertFalse(passwordService.needsUpgrade("not-a-supported-hash"));
    }

    private String legacyHash(String rawPassword) {
        return DigestUtils.md5DigestAsHex(
                ("ruyi_teach" + rawPassword).getBytes(StandardCharsets.UTF_8)
        );
    }
}
