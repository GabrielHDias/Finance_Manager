package io.github.gabrielhdias.financeManager.domain.auth;

import io.github.gabrielhdias.financeManager.api.auth.dto.LoginResponse;
import io.github.gabrielhdias.financeManager.domain.exception.InvalidCredentialsException;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            passwordEncoder,
            jwtEncoder,
            3600L
        );
    }

    @Test
    void shouldLoginWhenCredentialsAreValid() {
        String email = "gabriel@email.com";
        String rawPassword = "senha123";
        String passwordHash = "$2a$10$encodedPassword";

        User user = mock(User.class);
        Jwt jwt = mock(Jwt.class);

        when(user.getId())
            .thenReturn(1L);

        when(user.getEmail())
            .thenReturn(email);

        when(user.getPasswordHash())
            .thenReturn(passwordHash);

        when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
            rawPassword,
            passwordHash
        )).thenReturn(true);

        when(jwt.getTokenValue())
            .thenReturn("jwt-token");

        when(jwtEncoder.encode(any()))
            .thenReturn(jwt);

        LoginResponse response = authService.login(
            email,
            rawPassword
        );

        assertNotNull(response);

        assertEquals(
            "jwt-token",
            response.accessToken()
        );

        assertEquals(
            "Bearer",
            response.tokenType()
        );

        assertEquals(
            3600L,
            response.expiresIn()
        );

        verify(userRepository)
            .findByEmail(email);

        verify(passwordEncoder).matches(
            rawPassword,
            passwordHash
        );

        verify(jwtEncoder)
            .encode(any());
    }

    @Test
    void shouldNormalizeEmailBeforeLogin() {
        String rawEmail = "  GABRIEL@EMAIL.COM  ";
        String normalizedEmail = "gabriel@email.com";

        User user = mock(User.class);
        Jwt jwt = mock(Jwt.class);

        when(user.getId())
            .thenReturn(1L);

        when(user.getEmail())
            .thenReturn(normalizedEmail);

        when(user.getPasswordHash())
            .thenReturn("hash");

        when(userRepository.findByEmail(normalizedEmail))
            .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
            "senha123",
            "hash"
        )).thenReturn(true);

        when(jwt.getTokenValue())
            .thenReturn("jwt-token");

        when(jwtEncoder.encode(any()))
            .thenReturn(jwt);

        authService.login(
            rawEmail,
            "senha123"
        );

        verify(userRepository)
            .findByEmail(normalizedEmail);
    }

    @Test
    void shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
        String email = "naoexiste@email.com";

        when(userRepository.findByEmail(email))
            .thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> authService.login(
                email,
                "senha123"
            )
        );

        assertEquals(
            "Email ou senha inválidos",
            exception.getMessage()
        );

        verify(passwordEncoder, never())
            .matches(anyString(), anyString());

        verify(jwtEncoder, never())
            .encode(any());
    }

    @Test
    void shouldThrowInvalidCredentialsWhenPasswordIsIncorrect() {
        String email = "gabriel@email.com";

        User user = mock(User.class);

        when(user.getPasswordHash())
            .thenReturn("hash");

        when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
            "senhaErrada",
            "hash"
        )).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> authService.login(
                email,
                "senhaErrada"
            )
        );

        assertEquals(
            "Email ou senha inválidos",
            exception.getMessage()
        );

        verify(jwtEncoder, never())
            .encode(any());
    }
}
