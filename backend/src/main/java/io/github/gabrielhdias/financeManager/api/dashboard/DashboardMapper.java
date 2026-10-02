package io.github.gabrielhdias.financeManager.api.dashboard;

import io.github.gabrielhdias.financeManager.api.dashboard.dto.CategoryExpenseResponse;
import io.github.gabrielhdias.financeManager.api.dashboard.dto.DashboardSummaryResponse;
import io.github.gabrielhdias.financeManager.domain.dashboard.CategoryExpenseSummary;
import io.github.gabrielhdias.financeManager.domain.dashboard.FinancialSummary;

public final class DashboardMapper {

    private DashboardMapper() {
    }

    public static DashboardSummaryResponse toResponse(
        FinancialSummary summary
    ) {
        return new DashboardSummaryResponse(
            summary.totalIncome(),
            summary.totalExpense(),
            summary.balance()
        );
    }

    public static CategoryExpenseResponse toResponse(
        CategoryExpenseSummary summary
    ) {
        return new CategoryExpenseResponse(
            summary.categoryId(),
            summary.categoryName(),
            summary.amount()
        );
    }
}
