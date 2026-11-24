package com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow;

import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenVersion;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenVersionService;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// Pins linking to the same unexpired browser identity that explicitly started it.
@RequiredArgsConstructor
public class GoogleLinkVerifier {
    private final AuthTokenCookies cookies;
    private final JwtDecoder decoder;
    private final ResetTokenCodec hashes;
    private final AccessTokenVersionService versions;

    public void verify(HttpServletRequest request, GoogleFlowSession.LinkIntent intent) {
        String access = cookies.read(request);
        if (access == null
                || !MessageDigest.isEqual(
                        hashes.hash(access).getBytes(StandardCharsets.US_ASCII),
                        intent.accessHash().getBytes(StandardCharsets.US_ASCII))) {
            throw new GoogleLoginException(GoogleLoginException.Reason.FLOW_EXPIRED);
        }
        var jwt = decoder.decode(access);
        if (!intent.accountId().toString().equals(jwt.getSubject())) {
            throw new GoogleLoginException(GoogleLoginException.Reason.FLOW_EXPIRED);
        }
        versions.requireCurrent(intent.accountId(), AccessTokenVersion.read(jwt));
    }
}
