package io.github.gabrielhdias.financeManager.domain.budget;

import io.github.gabrielhdias.financeManager.domain.category.CategoryRepository;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BudgetService {
    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;

    public BudgetService(BudgetRepository budgets, CategoryRepository categories, TransactionRepository transactions) {
        this.budgets = budgets;
        this.categories = categories;
        this.transactions = transactions;
    }

    private YearMonth period(int year, int month) {
        if (year < 1 || year > 9999 || month < 1 || month > 12) {
            throw new IllegalArgumentException("Informe um mês e um ano válidos.");
        }
        return YearMonth.of(year, month);
    }

    @Transactional(readOnly = true)
    public List<BudgetSummary> list(Long userId, int year, int month) {
        YearMonth period = period(year, month);
        Map<Long, BigDecimal> expenses = transactions.sumExpensesByCategory(
            userId, TransactionType.EXPENSE, period.atDay(1), period.atEndOfMonth()
        ).stream().collect(Collectors.toMap(row -> row.categoryId(), row -> row.amount()));
        return budgets.findForMonth(userId, period.atDay(1)).stream().map(budget -> {
            BigDecimal spent = expenses.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
            return new BudgetSummary(budget.getId(), budget.getCategory().getId(), budget.getCategory().getName(),
                budget.getPeriodStart(), budget.getLimitAmount(), spent, budget.getLimitAmount().subtract(spent));
        }).toList();
    }

    @Transactional
    public BudgetSummary save(Long userId, Long categoryId, int year, int month, BigDecimal amount) {
        YearMonth period = period(year, month);
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.precision() - amount.scale() > 17) {
            throw new IllegalArgumentException("Informe um limite positivo com até duas casas decimais.");
        }
        categories.findByIdAndUserId(categoryId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        budgets.saveLimit(categoryId, period.atDay(1), amount);
        return list(userId, year, month).stream().filter(row -> row.categoryId().equals(categoryId)).findFirst().orElseThrow();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Budget budget = budgets.findByIdAndCategoryUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado"));
        budgets.delete(budget);
    }
}
