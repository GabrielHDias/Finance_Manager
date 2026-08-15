package io.github.gabrielhdias.financeManager.domain.transaction;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.type = type;
        this.account = account;
        this.category = category;
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
        this.description = description;
    }

    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void updateDate(LocalDate date) {
        this.date = date;
    }

    public void updateType(TransactionType type) {
        this.type = type;
    }

    public void updateAccount(Account account) {
        this.account = account;
    }

    public void updateCategory(Category category) {
        this.category = category;
    }
}
