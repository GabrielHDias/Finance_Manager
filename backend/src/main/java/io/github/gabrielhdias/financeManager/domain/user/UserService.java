package io.github.gabrielhdias.financeManager.domain.user;

import io.github.gabrielhdias.financeManager.domain.exception.DuplicateResourceException;
import io.github.gabrielhdias.financeManager.domain.exception.InvalidCredentialsException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public UserService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        EntityManager entityManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Transactional
    public User register(
        String name,
        String email,
        String rawPassword
    ) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException(
                "Já existe um usuário com esse email"
            );
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException(
                "A senha não pode estar vazia"
            );
        }

        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                "A senha é muito longa. Use uma senha menor, especialmente se incluir acentos ou emojis."
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
    public User findById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    @Transactional
    public User changeEmail(Long userId, String email, String currentPassword) {
        User user = requirePassword(userId, currentPassword);
        String normalized = normalizeEmail(email);
        if (normalized.equals(user.getEmail())) {
            return user;
        }
        if (userRepository.existsByEmail(normalized)) {
            throw new DuplicateResourceException("Já existe um usuário com esse email");
        }
        user.changeEmail(normalized);
        return user;
    }

    @Transactional
    public void deleteOwnAccount(Long userId, String currentPassword) {
        requirePassword(userId, currentPassword);
        for (String sql : new String[] {
            "DELETE FROM recurring_occurrences WHERE rule_id IN (SELECT id FROM recurring_rules WHERE account_id IN (SELECT id FROM accounts WHERE user_id = ?1))",
            "DELETE FROM recurring_rules WHERE account_id IN (SELECT id FROM accounts WHERE user_id = ?1)",
            "DELETE FROM transaction_tags WHERE transaction_id IN (SELECT t.id FROM transactions t JOIN accounts a ON t.account_id = a.id OR t.destination_account_id = a.id WHERE a.user_id = ?1)",
            "DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE user_id = ?1) OR destination_account_id IN (SELECT id FROM accounts WHERE user_id = ?1)",
            "DELETE FROM budget_month_skips WHERE category_id IN (SELECT id FROM categories WHERE user_id = ?1)",
            "DELETE FROM monthly_budgets WHERE category_id IN (SELECT id FROM categories WHERE user_id = ?1)",
            "DELETE FROM categories WHERE user_id = ?1",
            "DELETE FROM accounts WHERE user_id = ?1",
            "DELETE FROM tags WHERE user_id = ?1",
            "DELETE FROM users WHERE id = ?1"
        }) {
            var query = entityManager.createNativeQuery(sql);
            query.setParameter(1, userId);
            query.executeUpdate();
        }
    }

    private User requirePassword(Long userId, String currentPassword) {
        User user = findById(userId);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Senha atual inválida");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ResourceNotFoundException(
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
