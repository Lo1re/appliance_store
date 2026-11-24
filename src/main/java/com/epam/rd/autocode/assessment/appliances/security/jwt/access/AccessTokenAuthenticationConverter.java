package com.epam.rd.autocode.assessment.appliances.security.jwt.access;

import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;

public class AccessTokenAuthenticationConverter
        implements Converter<Jwt, AccessTokenAuthentication> {
    @Override
    public AccessTokenAuthentication convert(Jwt jwt) {
        var roles = jwt.getClaimAsStringList("roles");
        var authorities =
                Arrays.stream(AccountRole.values())
                        .filter(role -> roles.contains(role.name()))
                        .map(
                                role ->
                                        new org.springframework.security.core.authority
                                                .SimpleGrantedAuthority(role.authority()))
                        .toList();
        return new AccessTokenAuthentication(
                new AccessTokenPrincipal(Long.parseLong(jwt.getSubject())),
                authorities,
                jwt.getExpiresAt(),
                AccessTokenVersion.read(jwt));
    }
}
