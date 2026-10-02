package io.github.gabrielhdias.financeManager.domain.dashboard;

import java.math.BigDecimal;

public record FinancialSummary(
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal balance
) {
}
