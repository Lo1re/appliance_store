package com.epam.rd.autocode.assessment.appliances.security.identity;

import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenAuthentication;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenVersionService;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CurrentActor {

    private final UserRepository userRepository;
    private final AccessTokenVersionService versions;

    @Transactional(readOnly = true)
    public boolean hasRole(AccountRole expectedRole) {
        return AccountRole.from(requireUser()).filter(expectedRole::equals).isPresent();
    }

    @Transactional(readOnly = true)
    public User requireUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof AccountPrincipal principal)) {
            throw new AccessDeniedException("Access denied");
        }

        if (principal.getAccountId() == null) {
            throw new AccessDeniedException("Access denied");
        }

        User user = userRepository.findById(principal.getAccountId())
                .orElseThrow(() ->
                        new AccessDeniedException("Access denied")
                );

        AccountRole role = AccountRole.from(user)
                .orElseThrow(() ->
                        new AccessDeniedException("Access denied")
                );

        var declaredRoles = EnumSet.noneOf(AccountRole.class);
        for (AccountRole candidate : AccountRole.values()) {
            if (authentication.getAuthorities().stream()
                    .anyMatch(authority -> candidate.authority().equals(authority.getAuthority()))) {
                declaredRoles.add(candidate);
            }
        }
        if (!declaredRoles.equals(EnumSet.of(role))) {
            throw new AccessDeniedException("Access denied");
        }

        if (authentication instanceof AccessTokenAuthentication access)
            versions.requireCurrent(principal.getAccountId(), access.getCredentialVersion());
        return user;
    }
}
