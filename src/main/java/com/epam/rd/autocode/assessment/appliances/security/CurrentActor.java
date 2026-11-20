package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
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
                instanceof StorePrincipal principal)) {
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

        boolean roleMatches = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        role.authority().equals(authority.getAuthority())
                );

        if (!roleMatches) {
            throw new AccessDeniedException("Access denied");
        }

        return user;
    }
}
