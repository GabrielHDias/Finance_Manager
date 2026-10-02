package io.github.gabrielhdias.financeManager.api.dashboard;

import io.github.gabrielhdias.financeManager.api.dashboard.dto.CategoryExpenseResponse;
import io.github.gabrielhdias.financeManager.api.dashboard.dto.DashboardSummaryResponse;
import io.github.gabrielhdias.financeManager.domain.dashboard.CategoryExpenseSummary;
import io.github.gabrielhdias.financeManager.domain.dashboard.DashboardService;
import io.github.gabrielhdias.financeManager.domain.dashboard.FinancialSummary;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthenticatedUser authenticatedUser;

    public DashboardController(
        DashboardService dashboardService,
        AuthenticatedUser authenticatedUser
    ) {
        this.dashboardService = dashboardService;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
    ) {
        Long userId = authenticatedUser.getId();

        FinancialSummary summary =
            dashboardService.getSummary(
                userId,
                startDate,
                endDate
            );

        return ResponseEntity.ok(
            DashboardMapper.toResponse(summary)
        );
    }

    @GetMapping("/expenses-by-category")
    public ResponseEntity<List<CategoryExpenseResponse>>
    getExpensesByCategory(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
    ) {
        Long userId = authenticatedUser.getId();

        List<CategoryExpenseSummary> summaries =
            dashboardService.getExpensesByCategory(
                userId,
                startDate,
                endDate
            );

        List<CategoryExpenseResponse> response =
            summaries
                .stream()
                .map(DashboardMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }
}
