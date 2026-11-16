package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.mapper.OrderMapper;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.OrderAccessPolicy;
import com.epam.rd.autocode.assessment.appliances.security.StorePrincipal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {
    private final UserRepository users = mock(UserRepository.class);
    private final OrdersRepository orders = mock(OrdersRepository.class);
    private final OrderServiceImpl service = new OrderServiceImpl(
            orders,
            new CurrentActor(users),
            new OrderAccessPolicy(),
            mock(ClientRepository.class),
            new OrderRowsFactory(mock(ApplianceRepository.class)),
            Mappers.getMapper(OrderMapper.class)
    );

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMissingUnauthenticatedAndUnexpectedPrincipals() {
        denied();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("client", "unused")
        );
        denied();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("client", null, java.util.List.of())
        );
        denied();
        verifyNoInteractions(users, orders);
    }

    @Test
    void rejectsDeletedUnsupportedAndRoleMismatchedProfiles() {
        signIn("EMPLOYEE");
        when(users.findById(3L)).thenReturn(Optional.empty());
        denied();
        when(users.findById(3L)).thenReturn(Optional.of(new User()));
        denied();
        when(users.findById(3L)).thenReturn(Optional.of(new Client()));
        denied();
        verifyNoInteractions(orders);
    }

    @Test
    void clientCannotApproveEvenIfCalledOutsideTheSpringProxy() {
        signIn("CLIENT");
        when(users.findById(3L)).thenReturn(Optional.of(new Client()));

        assertThatThrownBy(() -> service.approve(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
        verifyNoInteractions(orders);
    }
    @Test
    void rejectsPrincipalWithoutAccountId() {
        var principal = new StorePrincipal(
                null,
                "test@example.com",
                "unused",
                "CLIENT"
        );

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );

        denied();

        verifyNoInteractions(users, orders);
    }
    private void signIn(String role) {
        var principal = new StorePrincipal(3L, "test", "unused", role);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()
                )
        );
    }

    private void denied() {
        assertThatThrownBy(() -> service.findById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }
}
