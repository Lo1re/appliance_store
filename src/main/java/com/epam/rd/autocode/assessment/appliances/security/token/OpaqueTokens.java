package com.epam.rd.autocode.assessment.appliances.security.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

// Shared 256-bit opaque-token encoding, canonical validation and storage fingerprinting.
public final class OpaqueTokens {
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");

    private OpaqueTokens() {}

    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static boolean isCanonical(String token) {
        if (token == null || !TOKEN_FORMAT.matcher(token).matches()) {
            return false;
        }
        byte[] bytes = Base64.getUrlDecoder().decode(token);
        return bytes.length == TOKEN_BYTES
                && Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(token);
    }

    // Accepts arbitrary identifiers too; format validation belongs to the credential boundary.
    public static String fingerprint(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }
}
