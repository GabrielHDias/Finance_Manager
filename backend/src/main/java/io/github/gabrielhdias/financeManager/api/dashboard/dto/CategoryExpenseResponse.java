package io.github.gabrielhdias.financeManager.api.dashboard.dto;

import java.math.BigDecimal;

public record CategoryExpenseResponse(
    Long categoryId,
    String categoryName,
    BigDecimal amount
) {
}
