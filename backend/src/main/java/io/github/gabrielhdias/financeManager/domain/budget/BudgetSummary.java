package io.github.gabrielhdias.financeManager.domain.budget;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetSummary(Long id, Long categoryId, String categoryName, LocalDate periodStart,
                            BigDecimal limitAmount, BigDecimal spent, BigDecimal remaining) {}
