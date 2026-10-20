package io.github.gabrielhdias.financeManager.domain.dashboard;

import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
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

    @Transactional(readOnly = true)
    public TransactionChartSummary getTransactionsSummary(Long userId, TransactionType type,
            ChartGrouping groupBy, LocalDate startDate, LocalDate endDate) {
        if (type == null || groupBy == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("Informe movimentação, agrupamento e as duas datas.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
        if (startDate.getYear() < 1 || endDate.getYear() > 9999) {
            throw new IllegalArgumentException("Informe datas entre os anos 0001 e 9999.");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if ((groupBy == ChartGrouping.DAY && days > 366) || endDate.isAfter(startDate.plusYears(10))) {
            throw new IllegalArgumentException("Selecione no máximo 366 dias no agrupamento diário ou 10 anos nos demais.");
        }

        List<TransactionChartSummary.Item> items;
        if (groupBy == ChartGrouping.CATEGORY) {
            // Reuse the existing query: it already accepts either financial type and groups by ID.
            items = transactionRepository.sumExpensesByCategory(userId, type, startDate, endDate)
                .stream().map(row -> new TransactionChartSummary.Item(row.categoryId().toString(),
                    row.categoryName(), row.amount())).toList();
        } else {
            var totals = new HashMap<String, BigDecimal>();
            if (groupBy == ChartGrouping.DAY) {
                transactionRepository.sumByDay(userId, type, startDate, endDate)
                    .forEach(row -> totals.put(row.date().toString(), row.amount()));
            } else {
                transactionRepository.sumByMonth(userId, type, startDate, endDate)
                    .forEach(row -> totals.put(YearMonth.of(row.year(), row.month()).toString(), row.amount()));
            }
            var filled = new ArrayList<TransactionChartSummary.Item>();
            var formatter = DateTimeFormatter.ofPattern(
                groupBy == ChartGrouping.DAY ? "dd/MM/yyyy" : "MMMM/yyyy", Locale.forLanguageTag("pt-BR"));
            LocalDate cursor = groupBy == ChartGrouping.DAY ? startDate : startDate.withDayOfMonth(1);
            while (!cursor.isAfter(endDate)) {
                String key = groupBy == ChartGrouping.DAY ? cursor.toString() : YearMonth.from(cursor).toString();
                filled.add(new TransactionChartSummary.Item(key, cursor.format(formatter),
                    totals.getOrDefault(key, BigDecimal.ZERO)));
                cursor = groupBy == ChartGrouping.DAY ? cursor.plusDays(1) : cursor.plusMonths(1);
            }
            items = List.copyOf(filled);
        }
        BigDecimal total = items.stream().map(TransactionChartSummary.Item::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TransactionChartSummary(type, groupBy, total, items);
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
