package io.github.gabrielhdias.financeManager.domain.transaction;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    protected Transaction() {
    }

    public Transaction(
        String description,
        BigDecimal amount,
        LocalDate date,
        TransactionType type,
        Account account,
        Category category
    ) {
        this.description = validateDescription(description);
        this.amount = validateAmount(amount);
        this.date = Objects.requireNonNull(
            date,
            "A data da transação não pode ser nula"
        );
        this.type = Objects.requireNonNull(
            type,
            "O tipo da transação não pode ser nulo"
        );
        this.account = Objects.requireNonNull(
            account,
            "A conta da transação não pode ser nula"
        );
        this.category = Objects.requireNonNull(
            category,
            "A categoria da transação não pode ser nula"
        );
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public TransactionType getType() {
        return type;
    }

    public Account getAccount() {
        return account;
    }

    public Category getCategory() {
        return category;
    }

    public void updateDescription(String description) {
        this.description = validateDescription(description);
    }

    public void updateAmount(BigDecimal amount) {
        this.amount = validateAmount(amount);
    }

    public void updateDate(LocalDate date) {
        this.date = Objects.requireNonNull(
            date,
            "A data da transação não pode ser nula"
        );
    }

    public void updateType(TransactionType type) {
        this.type = Objects.requireNonNull(
            type,
            "O tipo da transação não pode ser nulo"
        );
    }

    public void updateAccount(Account account) {
        this.account = Objects.requireNonNull(
            account,
            "A conta da transação não pode ser nula"
        );
    }

    public void updateCategory(Category category) {
        this.category = Objects.requireNonNull(
            category,
            "A categoria da transação não pode ser nula"
        );
    }

    private String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                "A descrição da transação não pode estar vazia"
            );
        }

        return description.trim();
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException(
                "O valor da transação não pode ser nulo"
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "O valor da transação deve ser maior que zero"
            );
        }

        return amount;
    }
}
