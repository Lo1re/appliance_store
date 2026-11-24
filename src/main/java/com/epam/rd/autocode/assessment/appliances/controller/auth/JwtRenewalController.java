package com.epam.rd.autocode.assessment.appliances.controller.auth;

import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.JwtCookieLifecycle;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.InvalidRefreshTokenException;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

// Browser-only HTML presentation of renewal; no token value enters the model.
@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.security.web-auth", havingValue = "jwt", matchIfMissing = true)
public class JwtRenewalController {
    private final RefreshTokenService refresh;
    private final AuthTokenCookies browserCookies;
    private final JwtCookieLifecycle cookies;

    @GetMapping("/auth/renew")
    public String form(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return "login/renew";
    }

    @PostMapping("/auth/renew")
    public String renew(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        try {
            String raw;
            try {
                raw = browserCookies.readRefresh(request);
            } catch (IllegalArgumentException malformed) {
                throw new InvalidRefreshTokenException();
            }
            cookies.issue(request, response, refresh.rotate(raw));
            return "redirect:/";
        } catch (InvalidRefreshTokenException rejected) {
            cookies.clear(request, response);
            return "redirect:/login?expired";
        }
    }
}
