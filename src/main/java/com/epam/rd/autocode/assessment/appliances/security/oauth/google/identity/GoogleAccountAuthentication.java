package com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity;

import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenPrincipal;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GoogleAccountAuthentication {
    private final UserRepository users;

    @Transactional(readOnly = true)
    public Authentication forAccount(Long id) {
        var user =
                users.findById(id)
                        .orElseThrow(
                                () ->
                                        new GoogleLoginException(
                                                GoogleLoginException.Reason.INVALID_IDENTITY));
        var role =
                AccountRole.from(user)
                        .orElseThrow(
                                () ->
                                        new GoogleLoginException(
                                                GoogleLoginException.Reason.INVALID_IDENTITY));
        return new UsernamePasswordAuthenticationToken(
                new AccessTokenPrincipal(id),
                null,
                List.of(new SimpleGrantedAuthority(role.authority())));
    }
}
