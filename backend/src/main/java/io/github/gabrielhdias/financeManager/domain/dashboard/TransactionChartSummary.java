package io.github.gabrielhdias.financeManager.domain.dashboard;

import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.util.List;

public record TransactionChartSummary(TransactionType type, ChartGrouping groupBy,
                                      BigDecimal total, List<Item> items) {
    public record Item(String key, String label, BigDecimal total) {}
}
