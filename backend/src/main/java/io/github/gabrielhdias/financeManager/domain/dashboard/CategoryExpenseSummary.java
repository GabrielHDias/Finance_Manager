package io.github.gabrielhdias.financeManager.domain.dashboard;

import java.math.BigDecimal;

public record CategoryExpenseSummary(
    Long categoryId,
    String categoryName,
    BigDecimal amount
) {
}
