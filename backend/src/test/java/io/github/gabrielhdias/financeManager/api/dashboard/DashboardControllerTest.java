package io.github.gabrielhdias.financeManager.api.dashboard;

import io.github.gabrielhdias.financeManager.api.dashboard.dto.DashboardSummaryResponse;
import io.github.gabrielhdias.financeManager.domain.dashboard.DashboardService;
import io.github.gabrielhdias.financeManager.domain.dashboard.FinancialSummary;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private AuthenticatedUser authenticatedUser;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    void shouldReturnFinancialSummary() {
        Long userId = 1L;

        LocalDate startDate =
            LocalDate.of(2026, 10, 1);

        LocalDate endDate =
            LocalDate.of(2026, 10, 31);

        FinancialSummary summary =
            new FinancialSummary(
                new BigDecimal("5000.00"),
                new BigDecimal("1850.50"),
                new BigDecimal("3149.50")
            );

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(dashboardService.getSummary(
            userId,
            startDate,
            endDate
        )).thenReturn(summary);

        ResponseEntity<DashboardSummaryResponse> response =
            dashboardController.getSummary(
                startDate,
                endDate
            );

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            new BigDecimal("5000.00"),
            response.getBody().totalIncome()
        );

        assertEquals(
            new BigDecimal("1850.50"),
            response.getBody().totalExpense()
        );

        assertEquals(
            new BigDecimal("3149.50"),
            response.getBody().balance()
        );

        verify(authenticatedUser)
            .getId();

        verify(dashboardService)
            .getSummary(
                userId,
                startDate,
                endDate
            );
    }
}
