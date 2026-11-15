package io.github.gabrielhdias.financeManager.domain.recurring;

import io.github.gabrielhdias.financeManager.domain.transaction.Transaction;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class RecurringGenerationService {
    private final RecurringRuleRepository rules;
    private final RecurringOccurrenceRepository occurrences;
    private final TransactionRepository transactions;
    private final Clock clock;

    public RecurringGenerationService(RecurringRuleRepository rules, RecurringOccurrenceRepository occurrences,
                                      TransactionRepository transactions, Clock clock) {
        this.rules = rules;
        this.occurrences = occurrences;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Transactional
    public void generate(Long userId) {
        LocalDate today = LocalDate.now(clock);
        for (RecurringRule rule : rules.findByAccountUserIdAndActiveTrue(userId)) {
            LocalDate limit = rule.getEndDate() == null || rule.getEndDate().isAfter(today) ? today : rule.getEndDate();
            LocalDate cursor = rule.getStartDate();
            while (!cursor.isAfter(limit)) {
                if (!occurrences.existsByRuleIdAndOccurrenceDate(rule.getId(), cursor)) {
                    Transaction transaction = transactions.save(new Transaction(
                        rule.getDescription(), rule.getAmount(), cursor, rule.getType(), rule.getAccount(), rule.getCategory()
                    ));
                    occurrences.save(new RecurringOccurrence(rule, cursor, transaction));
                }
                cursor = advance(rule, cursor);
            }
        }
    }

    private LocalDate advance(RecurringRule rule, LocalDate cursor) {
        if (rule.getFrequency() == RecurringFrequency.WEEKLY) {
            return cursor.plusWeeks(1);
        }
        YearMonth next = YearMonth.from(cursor).plusMonths(1);
        return next.atDay(Math.min(rule.getDayOfMonth(), next.lengthOfMonth()));
    }

    @Transactional(readOnly = true)
    public List<RecurringRule> list(Long userId) {
        return rules.findByAccountUserIdAndActiveTrue(userId);
    }
}
