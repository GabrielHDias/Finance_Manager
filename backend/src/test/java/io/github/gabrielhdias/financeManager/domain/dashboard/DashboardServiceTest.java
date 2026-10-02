package io.github.gabrielhdias.financeManager.domain.dashboard;

import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldCalculateFinancialSummary() {
        Long userId = 1L;

        LocalDate startDate =
            LocalDate.of(2026, 10, 1);

        LocalDate endDate =
            LocalDate.of(2026, 10, 31);

        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
            userId,
            TransactionType.INCOME,
            startDate,
            endDate
        )).thenReturn(
            new BigDecimal("5000.00")
        );

        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
            userId,
            TransactionType.EXPENSE,
            startDate,
            endDate
        )).thenReturn(
            new BigDecimal("1850.50")
        );

        FinancialSummary result =
            dashboardService.getSummary(
                userId,
                startDate,
                endDate
            );

        assertEquals(
            new BigDecimal("5000.00"),
            result.totalIncome()
        );

        assertEquals(
            new BigDecimal("1850.50"),
            result.totalExpense()
        );

        assertEquals(
            new BigDecimal("3149.50"),
            result.balance()
        );

        verify(transactionRepository)
            .sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.INCOME,
                startDate,
                endDate
            );

        verify(transactionRepository)
            .sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
            );
    }

    @Test
    void shouldReturnZeroSummaryWhenUserHasNoTransactions() {
        Long userId = 1L;

        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
            userId,
            TransactionType.INCOME,
            null,
            null
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
            userId,
            TransactionType.EXPENSE,
            null,
            null
        )).thenReturn(BigDecimal.ZERO);

        FinancialSummary result =
            dashboardService.getSummary(
                userId,
                null,
                null
            );

        assertEquals(
            BigDecimal.ZERO,
            result.totalIncome()
        );

        assertEquals(
            BigDecimal.ZERO,
            result.totalExpense()
        );

        assertEquals(
            BigDecimal.ZERO,
            result.balance()
        );
    }

    @Test
    void shouldNotCalculateSummaryWhenStartDateIsAfterEndDate() {
        Long userId = 1L;

        LocalDate startDate =
            LocalDate.of(2026, 10, 31);

        LocalDate endDate =
            LocalDate.of(2026, 10, 1);

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> dashboardService.getSummary(
                userId,
                startDate,
                endDate
            )
        );

        assertEquals(
            "A data inicial não pode ser posterior à data final",
            exception.getMessage()
        );

        verifyNoInteractions(transactionRepository);
    }
}
