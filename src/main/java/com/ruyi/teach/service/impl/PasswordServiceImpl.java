package com.ruyi.teach.service.impl;

import com.ruyi.teach.service.PasswordService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class PasswordServiceImpl implements PasswordService {

    private static final String LEGACY_SALT = "ruyi_teach";
    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    private static final Pattern LEGACY_MD5_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");

    private final PasswordEncoder passwordEncoder;

    public PasswordServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword must not be null");
        }
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (BCRYPT_PATTERN.matcher(storedPassword).matches()) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        if (!LEGACY_MD5_PATTERN.matcher(storedPassword).matches()) {
            return false;
        }

        String legacyHash = DigestUtils.md5DigestAsHex(
                (LEGACY_SALT + rawPassword).getBytes(StandardCharsets.UTF_8)
        );
        return MessageDigest.isEqual(
                legacyHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                storedPassword.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)
        );
    }

    @Override
    public boolean needsUpgrade(String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (LEGACY_MD5_PATTERN.matcher(storedPassword).matches()) {
            return true;
        }
        return BCRYPT_PATTERN.matcher(storedPassword).matches()
                && passwordEncoder.upgradeEncoding(storedPassword);
    }
}
