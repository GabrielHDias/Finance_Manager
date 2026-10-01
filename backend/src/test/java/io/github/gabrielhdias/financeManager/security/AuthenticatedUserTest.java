package io.github.gabrielhdias.financeManager.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticatedUserTest {

    private final AuthenticatedUser authenticatedUser =
        new AuthenticatedUser();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAuthenticatedUserIdFromJwtSubject() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("42")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        TestingAuthenticationToken authentication =
            new TestingAuthenticationToken(
                jwt,
                null
            );

        authentication.setAuthenticated(true);

        SecurityContextHolder
            .getContext()
            .setAuthentication(authentication);

        Long result = authenticatedUser.getId();

        assertEquals(
            42L,
            result
        );
    }

    @Test
    void shouldThrowExceptionWhenThereIsNoAuthentication() {
        SecurityContextHolder.clearContext();

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            authenticatedUser::getId
        );

        assertEquals(
            "Não existe usuário autenticado",
            exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenAuthenticationIsNotAuthenticated() {
        TestingAuthenticationToken authentication =
            new TestingAuthenticationToken(
                "principal",
                null
            );

        authentication.setAuthenticated(false);

        SecurityContextHolder
            .getContext()
            .setAuthentication(authentication);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            authenticatedUser::getId
        );

        assertEquals(
            "Não existe usuário autenticado",
            exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenPrincipalIsNotJwt() {
        TestingAuthenticationToken authentication =
            new TestingAuthenticationToken(
                "principal",
                null
            );

        authentication.setAuthenticated(true);

        SecurityContextHolder
            .getContext()
            .setAuthentication(authentication);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            authenticatedUser::getId
        );

        assertEquals(
            "O principal autenticado não é um JWT",
            exception.getMessage()
        );
    }
}
