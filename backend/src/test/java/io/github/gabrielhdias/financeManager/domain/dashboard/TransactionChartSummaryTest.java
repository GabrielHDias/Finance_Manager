package io.github.gabrielhdias.financeManager.domain.dashboard;

import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionChartSummaryTest {
    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final DashboardService service = new DashboardService(repository);
    private final LocalDate start = LocalDate.of(2025, 12, 31);
    private final LocalDate end = LocalDate.of(2026, 1, 2);

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void dailyFillsGapsAndSumsExactly(TransactionType type) {
        when(repository.sumByDay(7L, type, start, end)).thenReturn(List.of(
            new PeriodTotal(start, new BigDecimal("0.10")), new PeriodTotal(end, new BigDecimal("0.20"))));
        var result = service.getTransactionsSummary(7L, type, ChartGrouping.DAY, start, end);
        assertEquals(type, result.type());
        assertEquals(new BigDecimal("0.30"), result.total());
        assertEquals(List.of("2025-12-31", "2026-01-01", "2026-01-02"), result.items().stream().map(TransactionChartSummary.Item::key).toList());
        assertEquals("31/12/2025", result.items().getFirst().label());
        assertEquals(0, result.items().get(1).total().signum());
        verify(repository).sumByDay(7L, type, start, end);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void monthlyKeepsYearsAndFillsEmptyMonths() {
        LocalDate until = LocalDate.of(2027, 1, 2);
        when(repository.sumByMonth(7L, TransactionType.EXPENSE, start, until)).thenReturn(List.of(
            new MonthTotal(2026, 1, new BigDecimal("10.10")), new MonthTotal(2027, 1, new BigDecimal("20.20"))));
        var result = service.getTransactionsSummary(7L, TransactionType.EXPENSE, ChartGrouping.MONTH, start, until);
        assertEquals(14, result.items().size());
        assertEquals("2025-12", result.items().getFirst().key());
        assertEquals("janeiro/2026", result.items().get(1).label());
        assertEquals("2027-01", result.items().getLast().key());
        assertEquals(0, result.items().get(2).total().signum());
        assertEquals(new BigDecimal("30.30"), result.total());
    }

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void categoryReusesIdAggregationForBothTypes(TransactionType type) {
        when(repository.sumExpensesByCategory(7L, type, start, end)).thenReturn(List.of(
            new CategoryExpenseSummary(12L, "Categoria renomeada", new BigDecimal("199999999999999999.98"))));
        var result = service.getTransactionsSummary(7L, type, ChartGrouping.CATEGORY, start, end);
        assertEquals("12", result.items().getFirst().key());
        assertEquals("Categoria renomeada", result.items().getFirst().label());
        assertEquals(new BigDecimal("199999999999999999.98"), result.total());
    }

    @ParameterizedTest
    @EnumSource(ChartGrouping.class)
    void emptyPeriodsHaveZeroTotals(ChartGrouping group) {
        var result = service.getTransactionsSummary(7L, TransactionType.INCOME, group, start, end);
        assertEquals(0, result.total().signum());
        assertTrue(result.items().stream().allMatch(item -> item.total().signum() == 0));
        assertEquals(group == ChartGrouping.DAY ? 3 : group == ChartGrouping.MONTH ? 2 : 0, result.items().size());
    }

    @Test
    void rejectsMissingReversedAndExcessivePeriodsBeforeQuerying() {
        assertThrows(IllegalArgumentException.class, () -> service.getTransactionsSummary(7L, null, ChartGrouping.DAY, start, end));
        assertThrows(IllegalArgumentException.class, () -> service.getTransactionsSummary(7L, TransactionType.INCOME, null, start, end));
        assertThrows(IllegalArgumentException.class, () -> service.getTransactionsSummary(7L, TransactionType.INCOME, ChartGrouping.DAY, null, end));
        assertThrows(IllegalArgumentException.class, () -> service.getTransactionsSummary(7L, TransactionType.INCOME, ChartGrouping.DAY, start, null));
        assertThrows(IllegalArgumentException.class, () -> service.getTransactionsSummary(7L, TransactionType.INCOME, ChartGrouping.DAY, end, start));
        assertThrows(IllegalArgumentException.class, () -> service.getTransactionsSummary(7L, TransactionType.INCOME, ChartGrouping.DAY, start, start.plusDays(366)));
        assertThrows(IllegalArgumentException.class, () -> service.getTransactionsSummary(7L, TransactionType.INCOME, ChartGrouping.MONTH, start, start.plusYears(10).plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> service.getTransactionsSummary(7L, TransactionType.INCOME, ChartGrouping.MONTH, LocalDate.of(0, 1, 1), end));
        assertThrows(IllegalArgumentException.class, () -> service.getTransactionsSummary(7L, TransactionType.INCOME, ChartGrouping.MONTH, start, LocalDate.of(10000, 1, 1)));
        verifyNoInteractions(repository);
    }

    @Test
    void acceptsSingleDayAndDailyBoundary() {
        assertEquals(1, service.getTransactionsSummary(7L, TransactionType.EXPENSE, ChartGrouping.DAY, start, start).items().size());
        assertEquals(366, service.getTransactionsSummary(7L, TransactionType.EXPENSE, ChartGrouping.DAY, start, start.plusDays(365)).items().size());
    }
}
