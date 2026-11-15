package io.github.gabrielhdias.financeManager.domain.recurring;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_rules")
public class RecurringRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(nullable = false) private String description;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TransactionType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RecurringFrequency frequency;
    @Column(name = "day_of_month") private Integer dayOfMonth;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(nullable = false) private boolean active = true;

    protected RecurringRule() {}

    public RecurringRule(Account account, Category category, String description, BigDecimal amount, TransactionType type,
                         RecurringFrequency frequency, LocalDate startDate, LocalDate endDate) {
        this.account = account;
        this.category = category;
        this.description = description.trim();
        this.amount = amount;
        this.type = type;
        this.frequency = frequency;
        this.dayOfMonth = frequency == RecurringFrequency.MONTHLY ? startDate.getDayOfMonth() : null;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = true;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public Category getCategory() { return category; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public RecurringFrequency getFrequency() { return frequency; }
    public Integer getDayOfMonth() { return dayOfMonth; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isActive() { return active; }
    public void deactivate() { this.active = false; }
}
