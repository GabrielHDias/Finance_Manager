package io.github.gabrielhdias.financeManager.api.dashboard.dto;

import io.github.gabrielhdias.financeManager.api.money.MoneySerializer;
import io.github.gabrielhdias.financeManager.domain.dashboard.ChartGrouping;
import io.github.gabrielhdias.financeManager.domain.dashboard.TransactionChartSummary;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import tools.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record TransactionChartResponse(TransactionType type, ChartGrouping groupBy,
    @JsonSerialize(using = MoneySerializer.class) @Schema(type = "string", implementation = String.class, example = "0.30") BigDecimal total,
    List<Item> items) {
    public record Item(String key, String label,
        @JsonSerialize(using = MoneySerializer.class) @Schema(type = "string", implementation = String.class, example = "0.30") BigDecimal total) {}

    public static TransactionChartResponse from(TransactionChartSummary summary) {
        return new TransactionChartResponse(summary.type(), summary.groupBy(), summary.total(),
            summary.items().stream().map(item -> new Item(item.key(), item.label(), item.total())).toList());
    }
}
