package com.epam.rd.autocode.assessment.appliances.api;

import com.epam.rd.autocode.assessment.appliances.dto.auth.AccessTokenMetadata;
import com.epam.rd.autocode.assessment.appliances.dto.auth.ApiCsrfTokenResponse;
import com.epam.rd.autocode.assessment.appliances.dto.auth.ApiLoginRequest;
import com.epam.rd.autocode.assessment.appliances.security.jwt.ApiPasswordLogin;
import com.epam.rd.autocode.assessment.appliances.security.jwt.ApiTransportProperties;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtCookieLifecycle;
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
    private final JwtCookieLifecycle cookies;
    private final ApiTransportProperties transport;

    @GetMapping("/csrf")
    public ResponseEntity<ApiCsrfTokenResponse> csrf(CsrfToken csrf) {
        if (!transport.cookieMode() || csrf == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(new ApiCsrfTokenResponse(csrf.getHeaderName(), csrf.getParameterName(), csrf.getToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody ApiLoginRequest credentials,
                                   HttpServletRequest request, HttpServletResponse response) {
        var token = passwords.login(credentials.getEmail(), credentials.getPassword());
        if (transport.cookieMode()) {
            cookies.issue(request, response, token);
            return ResponseEntity.ok().header("Cache-Control", "no-store")
                    .body(new AccessTokenMetadata(token.expiresIn(), token.expiresAt()));
        }
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        cookies.clear(request, response); return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }
}
