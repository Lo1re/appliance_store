package com.epam.rd.autocode.assessment.appliances.security.jwt.refresh;

import com.epam.rd.autocode.assessment.appliances.security.jwt.config.ConditionalOnJwt;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Generates opaque 256-bit credentials and computes their one-way storage fingerprint. */
@Component
@ConditionalOnJwt
public class RefreshTokenCodec {
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public String hash(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw new InvalidRefreshTokenException();
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(token);
            if (bytes.length != 32 || !Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(token))
                throw new InvalidRefreshTokenException();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable"); }
        catch (IllegalArgumentException invalid) { throw new InvalidRefreshTokenException(); }
    }
}
