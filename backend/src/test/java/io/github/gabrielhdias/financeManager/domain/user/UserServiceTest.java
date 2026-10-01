package io.github.gabrielhdias.financeManager.domain.user;

import io.github.gabrielhdias.financeManager.domain.exception.DuplicateResourceException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterUserWithEncodedPassword() {
        String name = "Gabriel";
        String email = "gabriel@email.com";
        String rawPassword = "senha123";
        String passwordHash = "$2a$10$encodedPassword";

        when(userRepository.existsByEmail(email))
            .thenReturn(false);

        when(passwordEncoder.encode(rawPassword))
            .thenReturn(passwordHash);

        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(
            name,
            email,
            rawPassword
        );

        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals(email, result.getEmail());

        assertEquals(
            passwordHash,
            result.getPasswordHash()
        );

        assertNotEquals(
            rawPassword,
            result.getPasswordHash()
        );

        verify(passwordEncoder)
            .encode(rawPassword);

        verify(userRepository)
            .save(any(User.class));
    }

    @Test
    void shouldNormalizeEmailWhenRegisteringUser() {
        String rawEmail = "  GABRIEL@EMAIL.COM  ";
        String normalizedEmail = "gabriel@email.com";

        when(userRepository.existsByEmail(normalizedEmail))
            .thenReturn(false);

        when(passwordEncoder.encode("senha123"))
            .thenReturn("$2a$10$encodedPassword");

        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(
            "Gabriel",
            rawEmail,
            "senha123"
        );

        assertEquals(
            normalizedEmail,
            result.getEmail()
        );

        verify(userRepository)
            .existsByEmail(normalizedEmail);
    }

    @Test
    void shouldNotRegisterUserWhenEmailAlreadyExists() {
        String email = "gabriel@email.com";

        when(userRepository.existsByEmail(email))
            .thenReturn(true);

        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> userService.register(
                "Gabriel",
                email,
                "senha123"
            )
        );

        assertEquals(
            "Já existe um usuário com esse email",
            exception.getMessage()
        );

        verify(passwordEncoder, never())
            .encode(anyString());

        verify(userRepository, never())
            .save(any(User.class));
    }

    @Test
    void shouldNotRegisterUserWhenPasswordIsBlank() {
        String email = "gabriel@email.com";

        when(userRepository.existsByEmail(email))
            .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.register(
                "Gabriel",
                email,
                "   "
            )
        );

        assertEquals(
            "A senha não pode estar vazia",
            exception.getMessage()
        );

        verify(passwordEncoder, never())
            .encode(anyString());

        verify(userRepository, never())
            .save(any(User.class));
    }

    @Test
    void shouldFindUserByEmail() {
        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "$2a$10$encodedPassword"
        );

        when(userRepository.findByEmail(
            "gabriel@email.com"
        )).thenReturn(Optional.of(user));

        User result = userService.findByEmail(
            "gabriel@email.com"
        );

        assertEquals(user, result);

        verify(userRepository)
            .findByEmail("gabriel@email.com");
    }

    @Test
    void shouldNormalizeEmailWhenFindingUser() {
        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "$2a$10$encodedPassword"
        );

        when(userRepository.findByEmail(
            "gabriel@email.com"
        )).thenReturn(Optional.of(user));

        User result = userService.findByEmail(
            "  GABRIEL@EMAIL.COM "
        );

        assertEquals(user, result);

        verify(userRepository)
            .findByEmail("gabriel@email.com");
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotFoundByEmail() {
        String email = "gabriel@email.com";

        when(userRepository.findByEmail(email))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userService.findByEmail(email)
        );

        assertEquals(
            "Usuário não encontrado",
            exception.getMessage()
        );
    }
}
