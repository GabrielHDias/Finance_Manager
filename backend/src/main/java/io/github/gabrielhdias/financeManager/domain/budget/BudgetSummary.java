package io.github.gabrielhdias.financeManager.domain.budget;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.annotation.JsonSerialize;
import io.github.gabrielhdias.financeManager.api.money.MoneySerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record BudgetSummary(Long id, Long categoryId, String categoryName, LocalDate periodStart,
                            @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonSerialize(using = MoneySerializer.class) @Schema(type = "string", implementation = String.class, example = "0.30") BigDecimal limitAmount, @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonSerialize(using = MoneySerializer.class) @Schema(type = "string", implementation = String.class, example = "0.30") BigDecimal spent, @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonSerialize(using = MoneySerializer.class) @Schema(type = "string", implementation = String.class, example = "0.30") BigDecimal remaining) {}
