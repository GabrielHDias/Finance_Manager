package io.github.gabrielhdias.financeManager.api.dashboard;

import io.github.gabrielhdias.financeManager.config.security.SecurityConfig;
import io.github.gabrielhdias.financeManager.config.security.SecurityTestConfig;
import io.github.gabrielhdias.financeManager.domain.dashboard.CategoryExpenseSummary;
import io.github.gabrielhdias.financeManager.domain.dashboard.DashboardService;
import io.github.gabrielhdias.financeManager.domain.dashboard.FinancialSummary;
import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(SecurityTestConfig.class)
class DashboardControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnUnauthorizedWhenRequestHasNoToken()
        throws Exception {

        mockMvc.perform(
                get("/dashboard/summary")
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnSummaryWhenAuthenticated()
        throws Exception {

        Long userId = 1L;

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
            null,
            null
        )).thenReturn(summary);

        mockMvc.perform(
                get("/dashboard/summary")
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalIncome")
                .value(5000.00))
            .andExpect(jsonPath("$.totalExpense")
                .value(1850.50))
            .andExpect(jsonPath("$.balance")
                .value(3149.50));
    }

    @Test
    void shouldReturnSummaryWithPeriod()
        throws Exception {

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

        mockMvc.perform(
                get("/dashboard/summary")
                    .param(
                        "startDate",
                        "2026-10-01"
                    )
                    .param(
                        "endDate",
                        "2026-10-31"
                    )
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalIncome")
                .value(5000.00))
            .andExpect(jsonPath("$.totalExpense")
                .value(1850.50))
            .andExpect(jsonPath("$.balance")
                .value(3149.50));
    }

    @Test
    void shouldReturnConflictWhenPeriodIsInvalid()
        throws Exception {

        Long userId = 1L;

        LocalDate startDate =
            LocalDate.of(2026, 10, 31);

        LocalDate endDate =
            LocalDate.of(2026, 10, 1);

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(dashboardService.getSummary(
            userId,
            startDate,
            endDate
        )).thenThrow(
            new BusinessRuleException(
                "A data inicial não pode ser posterior à data final"
            )
        );

        mockMvc.perform(
                get("/dashboard/summary")
                    .param(
                        "startDate",
                        "2026-10-31"
                    )
                    .param(
                        "endDate",
                        "2026-10-01"
                    )
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status")
                .value(409))
            .andExpect(jsonPath("$.message")
                .value(
                    "A data inicial não pode ser posterior à data final"
                ));
    }

    @Test
    void shouldReturnExpensesByCategoryWhenAuthenticated()
        throws Exception {

        Long userId = 1L;

        List<CategoryExpenseSummary> summaries =
            List.of(
                new CategoryExpenseSummary(
                    10L,
                    "Alimentação",
                    new BigDecimal("850.00")
                ),
                new CategoryExpenseSummary(
                    20L,
                    "Transporte",
                    new BigDecimal("320.50")
                )
            );

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(dashboardService.getExpensesByCategory(
            userId,
            null,
            null
        )).thenReturn(summaries);

        mockMvc.perform(
                get("/dashboard/expenses-by-category")
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].categoryId")
                .value(10))
            .andExpect(jsonPath("$[0].categoryName")
                .value("Alimentação"))
            .andExpect(jsonPath("$[0].amount")
                .value(850.00))
            .andExpect(jsonPath("$[1].categoryId")
                .value(20))
            .andExpect(jsonPath("$[1].categoryName")
                .value("Transporte"))
            .andExpect(jsonPath("$[1].amount")
                .value(320.50));
    }

    @Test
    void shouldReturnExpensesByCategoryWithPeriod()
        throws Exception {

        Long userId = 1L;

        LocalDate startDate =
            LocalDate.of(2026, 10, 1);

        LocalDate endDate =
            LocalDate.of(2026, 10, 31);

        List<CategoryExpenseSummary> summaries =
            List.of(
                new CategoryExpenseSummary(
                    10L,
                    "Alimentação",
                    new BigDecimal("850.00")
                )
            );

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(dashboardService.getExpensesByCategory(
            userId,
            startDate,
            endDate
        )).thenReturn(summaries);

        mockMvc.perform(
                get("/dashboard/expenses-by-category")
                    .param(
                        "startDate",
                        "2026-10-01"
                    )
                    .param(
                        "endDate",
                        "2026-10-31"
                    )
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].categoryName")
                .value("Alimentação"))
            .andExpect(jsonPath("$[0].amount")
                .value(850.00));
    }

    @Test
    void shouldReturnConflictWhenExpenseCategoryPeriodIsInvalid()
        throws Exception {

        Long userId = 1L;

        LocalDate startDate =
            LocalDate.of(2026, 10, 31);

        LocalDate endDate =
            LocalDate.of(2026, 10, 1);

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(dashboardService.getExpensesByCategory(
            userId,
            startDate,
            endDate
        )).thenThrow(
            new BusinessRuleException(
                "A data inicial não pode ser posterior à data final"
            )
        );

        mockMvc.perform(
                get("/dashboard/expenses-by-category")
                    .param(
                        "startDate",
                        "2026-10-31"
                    )
                    .param(
                        "endDate",
                        "2026-10-01"
                    )
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status")
                .value(409))
            .andExpect(jsonPath("$.message")
                .value(
                    "A data inicial não pode ser posterior à data final"
                ));
    }
}
