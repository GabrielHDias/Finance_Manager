package io.github.gabrielhdias.financeManager.api.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal balance
) {
}
