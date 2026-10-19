package io.github.gabrielhdias.financeManager.domain.budget;

import io.github.gabrielhdias.financeManager.domain.category.Category;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "monthly_budgets")
public class Budget {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    protected Budget() {}
    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public LocalDate getPeriodStart() { return periodStart; }
    public BigDecimal getLimitAmount() { return limitAmount; }
}
