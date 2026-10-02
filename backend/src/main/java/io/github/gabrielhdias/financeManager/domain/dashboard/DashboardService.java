package io.github.gabrielhdias.financeManager.domain.dashboard;

import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;

    public DashboardService(
        TransactionRepository transactionRepository
    ) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public FinancialSummary getSummary(
        Long userId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        validatePeriod(
            startDate,
            endDate
        );

        BigDecimal totalIncome =
            transactionRepository
                .sumAmountByUserAndTypeAndPeriod(
                    userId,
                    TransactionType.INCOME,
                    startDate,
                    endDate
                );

        BigDecimal totalExpense =
            transactionRepository
                .sumAmountByUserAndTypeAndPeriod(
                    userId,
                    TransactionType.EXPENSE,
                    startDate,
                    endDate
                );

        BigDecimal balance =
            totalIncome.subtract(totalExpense);

        return new FinancialSummary(
            totalIncome,
            totalExpense,
            balance
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryExpenseSummary> getExpensesByCategory(
        Long userId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        validatePeriod(
            startDate,
            endDate
        );

        return transactionRepository.sumExpensesByCategory(
            userId,
            TransactionType.EXPENSE,
            startDate,
            endDate
        );
    }

    private void validatePeriod(
        LocalDate startDate,
        LocalDate endDate
    ) {
        if (startDate != null
            && endDate != null
            && startDate.isAfter(endDate)) {
            throw new BusinessRuleException(
                "A data inicial não pode ser posterior à data final"
            );
        }
    }
}
