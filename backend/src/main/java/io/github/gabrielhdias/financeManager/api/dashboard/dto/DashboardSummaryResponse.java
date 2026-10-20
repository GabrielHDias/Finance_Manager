package io.github.gabrielhdias.financeManager.api.dashboard.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.annotation.JsonSerialize;
import io.github.gabrielhdias.financeManager.api.money.MoneySerializer;
import io.swagger.v3.oas.annotations.media.Schema;

public record DashboardSummaryResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonSerialize(using = MoneySerializer.class) @Schema(type = "string", implementation = String.class, example = "0.30") BigDecimal totalIncome,
    @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonSerialize(using = MoneySerializer.class) @Schema(type = "string", implementation = String.class, example = "0.30") BigDecimal totalExpense,
    @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonSerialize(using = MoneySerializer.class) @Schema(type = "string", implementation = String.class, example = "0.30") BigDecimal balance
) {
}
