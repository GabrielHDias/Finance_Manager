package io.github.gabrielhdias.financeManager.api.transaction.dto;

import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
    Long id,
    String description,
    BigDecimal amount,
    LocalDate date,
    TransactionType type,
    Long accountId,
    String accountName,
    Long categoryId,
    String categoryName
) {
}
