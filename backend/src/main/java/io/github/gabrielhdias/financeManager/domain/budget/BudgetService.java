package io.github.gabrielhdias.financeManager.domain.budget;

import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryRepository;
import io.github.gabrielhdias.financeManager.domain.category.CategoryType;
import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BudgetService {
    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final BudgetMonthSkipRepository skips;

    public BudgetService(BudgetRepository budgets, CategoryRepository categories, TransactionRepository transactions,
                         BudgetMonthSkipRepository skips) {
        this.budgets = budgets;
        this.categories = categories;
        this.transactions = transactions;
        this.skips = skips;
    }

    private YearMonth period(int year, int month) {
        if (year < 1 || year > 9999 || month < 1 || month > 12) {
            throw new IllegalArgumentException("Informe um mês e um ano válidos.");
        }
        return YearMonth.of(year, month);
    }

    @Transactional
    public List<BudgetSummary> list(Long userId, int year, int month) {
        YearMonth period = period(year, month);
        carry(userId, period);
        Map<Long, BigDecimal> expenses = transactions.sumExpensesByCategory(
            userId, TransactionType.EXPENSE, period.atDay(1), period.atEndOfMonth()
        ).stream().collect(Collectors.toMap(row -> row.categoryId(), row -> row.amount()));
        return budgets.findForMonth(userId, period.atDay(1)).stream().map(budget -> {
            BigDecimal spent = expenses.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
            return new BudgetSummary(budget.getId(), budget.getCategory().getId(), budget.getCategory().getName(),
                budget.getPeriodStart(), budget.getLimitAmount(), spent, budget.getLimitAmount().subtract(spent));
        }).toList();
    }

    private void carry(Long userId, YearMonth requested) {
        var byCategory = budgets.findUpTo(userId, requested.atDay(1)).stream()
            .collect(java.util.stream.Collectors.groupingBy(budget -> budget.getCategory().getId()));
        byCategory.forEach((categoryId, rows) -> {
            rows.sort(java.util.Comparator.comparing(Budget::getPeriodStart));
            Budget source = null;
            for (Budget row : rows) {
                if (!row.getPeriodStart().isAfter(requested.atDay(1))) source = row.isRepeatsMonthly() ? row : null;
            }
            if (source == null || !source.isRepeatsMonthly()) return;
            YearMonth cursor = YearMonth.from(source.getPeriodStart()).plusMonths(1);
            while (!cursor.isAfter(requested)) {
                LocalDate start = cursor.atDay(1);
                if (skips.existsById(new BudgetMonthSkip.Key(categoryId, start))) return;
                boolean exists = rows.stream().anyMatch(row -> row.getPeriodStart().equals(start));
                if (!exists) {
                    budgets.saveLimit(categoryId, start, source.getLimitAmount(), true);
                }
                cursor = cursor.plusMonths(1);
            }
        });
    }

    public BudgetSummary save(Long userId, Long categoryId, int year, int month, BigDecimal amount) {
        return save(userId, categoryId, year, month, amount, false);
    }

    @Transactional
    public BudgetSummary save(Long userId, Long categoryId, int year, int month, BigDecimal amount, boolean repeatsMonthly) {
        YearMonth period = period(year, month);
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.precision() - amount.scale() > 17) {
            throw new IllegalArgumentException("Informe um limite positivo com até duas casas decimais.");
        }
        Category category = categories.findByIdAndUserId(categoryId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        if (category.getType() != CategoryType.EXPENSE) {
            throw new BusinessRuleException("Orçamento só pode ser definido para categorias de despesa.");
        }
        budgets.saveLimit(categoryId, period.atDay(1), amount, repeatsMonthly);
        BudgetMonthSkip.Key key = new BudgetMonthSkip.Key(categoryId, period.atDay(1));
        if (skips.existsById(key)) skips.deleteById(key);
        return list(userId, year, month).stream().filter(row -> row.categoryId().equals(categoryId)).findFirst().orElseThrow();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Budget budget = budgets.findByIdAndCategoryUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado"));
        skips.save(new BudgetMonthSkip(budget.getCategory().getId(), budget.getPeriodStart()));
        budgets.delete(budget);
    }
}
