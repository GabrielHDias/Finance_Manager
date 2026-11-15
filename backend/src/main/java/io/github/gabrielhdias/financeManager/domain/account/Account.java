package io.github.gabrielhdias.financeManager.domain.account;

import io.github.gabrielhdias.financeManager.domain.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingBalance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected Account() {
    }

    public Account(String name, User user) {
        this(name, BigDecimal.ZERO, user);
    }

    public Account(String name, BigDecimal openingBalance, User user) {
        this.name = validateName(name);
        this.openingBalance = validateOpeningBalance(openingBalance);
        this.user = Objects.requireNonNull(
            user,
            "O usuário da conta não pode ser nulo"
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public User getUser() {
        return user;
    }

    public void rename(String name) {
        this.name = validateName(name);
    }

    public void updateOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = validateOpeningBalance(openingBalance);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                "O nome da conta não pode estar vazio"
            );
        }

        return name.trim();
    }

    private BigDecimal validateOpeningBalance(BigDecimal openingBalance) {
        if (openingBalance == null) {
            throw new IllegalArgumentException(
                "O saldo inicial não pode ser nulo"
            );
        }
        if (openingBalance.scale() > 2 || openingBalance.precision() - openingBalance.scale() > 17) {
            throw new IllegalArgumentException(
                "O saldo inicial deve ter no máximo 17 dígitos inteiros e 2 casas decimais"
            );
        }
        return openingBalance.setScale(2);
    }
}
