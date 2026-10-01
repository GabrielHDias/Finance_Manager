package io.github.gabrielhdias.financeManager.domain.auth;

import io.github.gabrielhdias.financeManager.api.auth.dto.LoginResponse;
import io.github.gabrielhdias.financeManager.domain.exception.InvalidCredentialsException;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long expirationSeconds;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtEncoder jwtEncoder,
        @Value("${security.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.expirationSeconds = expirationSeconds;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(
        String email,
        String rawPassword
    ) {
        String normalizedEmail =
            email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new InvalidCredentialsException(
                "Email ou senha inválidos"
            ));

        if (!passwordEncoder.matches(
            rawPassword,
            user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException(
                "Email ou senha inválidos"
            );
        }

        Instant now = Instant.now();
        Instant expiresAt =
            now.plusSeconds(expirationSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .build();

        String token = jwtEncoder.encode(
            JwtEncoderParameters.from(claims)
        ).getTokenValue();

        return new LoginResponse(
            token,
            "Bearer",
            expirationSeconds
        );
    }
}
