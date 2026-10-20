package io.github.gabrielhdias.financeManager.api.transaction.dto;

import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.annotation.JsonSerialize;
import io.github.gabrielhdias.financeManager.api.money.MoneySerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record TransactionResponse(
    Long id,
    String description,
    @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonSerialize(using = MoneySerializer.class) @Schema(type = "string", implementation = String.class, example = "0.30") BigDecimal amount,
    LocalDate date,
    TransactionType type,
    Long accountId,
    String accountName,
    Long categoryId,
    String categoryName
) {
}
