package com.epam.rd.autocode.assessment.appliances.api.controller;

import com.epam.rd.autocode.assessment.appliances.dto.auth.AccessTokenMetadata;
import com.epam.rd.autocode.assessment.appliances.dto.auth.ApiCsrfTokenResponse;
import com.epam.rd.autocode.assessment.appliances.dto.auth.ApiLoginRequest;
import com.epam.rd.autocode.assessment.appliances.dto.auth.RefreshTokenRequest;
import com.epam.rd.autocode.assessment.appliances.dto.auth.TokenPairResponse;
import com.epam.rd.autocode.assessment.appliances.security.jwt.TokenPair;
import com.epam.rd.autocode.assessment.appliances.security.jwt.api.ApiPasswordLogin;
import com.epam.rd.autocode.assessment.appliances.security.jwt.api.ApiTransportProperties;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.JwtCookieLifecycle;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.InvalidRefreshTokenException;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
public class ApiAuthenticationController {
    private final ApiPasswordLogin passwords;
    private final RefreshTokenService refresh;
    private final AuthTokenCookies browserCookies;
    private final JwtCookieLifecycle cookies;
    private final ApiTransportProperties transport;

    @GetMapping("/csrf")
    public ResponseEntity<ApiCsrfTokenResponse> csrf(CsrfToken csrf) {
        if (!transport.cookieMode() || csrf == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(new ApiCsrfTokenResponse(csrf.getHeaderName(), csrf.getParameterName(), csrf.getToken()));
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody ApiLoginRequest credentials, HttpServletRequest request, HttpServletResponse response) {
        String previous = null;
        if (transport.cookieMode()) {
            try { previous = browserCookies.readRefresh(request); } catch (IllegalArgumentException malformed) { /* A password login can replace malformed browser credentials. */ }
        }
        return issued(passwords.login(credentials.getEmail(), credentials.getPassword(), previous), request, response);
    }
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody(required = false) RefreshTokenRequest credentials,
                                      HttpServletRequest request, HttpServletResponse response) {
        try { return issued(refresh.rotate(presented(credentials, request)), request, response); }
        catch (InvalidRefreshTokenException rejected) {
            if (transport.cookieMode()) cookies.clear(request, response);
            throw rejected;
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody(required = false) RefreshTokenRequest credentials,
                                        HttpServletRequest request, HttpServletResponse response) {
        String raw;
        try { raw = presented(credentials, request); } catch (InvalidRefreshTokenException malformed) { raw = null; }
        try { refresh.revoke(raw); } finally { if (transport.cookieMode()) cookies.clear(request, response); }
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }
    private String presented(RefreshTokenRequest body, HttpServletRequest request) {
        if (!transport.cookieMode()) return body == null ? null : body.refreshToken();
        if (body != null) throw new InvalidRefreshTokenException();
        try { return browserCookies.readRefresh(request); }
        catch (IllegalArgumentException malformed) { throw new InvalidRefreshTokenException(); }
    }
    private ResponseEntity<?> issued(TokenPair pair, HttpServletRequest request, HttpServletResponse response) {
        Object body;
        if (transport.cookieMode()) {
            cookies.issue(request, response, pair);
            body = new AccessTokenMetadata(pair.access().expiresIn(), pair.access().expiresAt());
        } else body = new TokenPairResponse(pair.access().accessToken(), pair.access().tokenType(), pair.access().expiresIn(),
                pair.access().expiresAt(), pair.refreshToken(), pair.refreshExpiresAt());
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(body);
    }
}
