package io.github.gabrielhdias.financeManager.domain.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(
        String name,
        String email,
        String rawPassword
    ) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException(
                "Já existe um usuário com esse email"
            );
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException(
                "A senha não pode estar vazia"
            );
        }

        String passwordHash =
            passwordEncoder.encode(rawPassword);

        User user = new User(
            name,
            normalizedEmail,
            passwordHash
        );

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException(
                "Usuário não encontrado"
            ));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "O email não pode estar vazio"
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
