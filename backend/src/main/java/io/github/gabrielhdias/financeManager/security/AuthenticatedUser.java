package io.github.gabrielhdias.financeManager.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {

    public Long getId() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();

        if (authentication == null
            || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                "Não existe usuário autenticado"
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Jwt jwt)) {
            throw new IllegalStateException(
                "O principal autenticado não é um JWT"
            );
        }

        return Long.valueOf(jwt.getSubject());
    }
}
