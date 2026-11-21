package com.epam.rd.autocode.assessment.appliances.security.jwt;

import com.epam.rd.autocode.assessment.appliances.dto.auth.AccessTokenResponse;
import com.epam.rd.autocode.assessment.appliances.security.AccountPrincipal;
import com.epam.rd.autocode.assessment.appliances.security.AccountRole;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnJwt
public class AccessTokenIssuer {
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public AccessTokenResponse issue(Authentication authentication) {
        var principal = (AccountPrincipal) authentication.getPrincipal();
        var roles = Arrays.stream(AccountRole.values()).filter(role -> authentication.getAuthorities().stream()
                .anyMatch(authority -> role.authority().equals(authority.getAuthority()))).map(Enum::name).toList();
        var now = clock.instant(); var expires = now.plus(properties.getTtl());
        var claims = JwtClaimsSet.builder().issuer(properties.getIssuer()).audience(List.of(properties.getAudience()))
                .subject(principal.getAccountId().toString()).issuedAt(now).notBefore(now).expiresAt(expires)
                .id(UUID.randomUUID().toString()).claim("roles", roles).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AccessTokenResponse(token, "Bearer", properties.getTtl().toSeconds(), expires);
    }
}
