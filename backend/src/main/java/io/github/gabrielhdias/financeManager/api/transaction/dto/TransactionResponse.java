package io.github.gabrielhdias.financeManager.api.transaction.dto;

import io.github.gabrielhdias.financeManager.api.tag.dto.TagResponse;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.annotation.JsonSerialize;
import io.github.gabrielhdias.financeManager.api.money.MoneySerializer;
import io.swagger.v3.oas.annotations.media.Schema;

public record TransactionResponse(
    Long id,
    String description,
    @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonSerialize(using = MoneySerializer.class) @Schema(type = "string", implementation = String.class, example = "0.30") BigDecimal amount,
    LocalDate date,
    TransactionType type,
    Long accountId,
    String accountName,
    Long categoryId,
    String categoryName,
    Long destinationAccountId,
    String destinationAccountName,
    List<TagResponse> tags
) {
}
