package io.github.gabrielhdias.financeManager.domain.recurring;

import io.github.gabrielhdias.financeManager.domain.transaction.Transaction;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "recurring_occurrences")
public class RecurringOccurrence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private RecurringRule rule;
    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    protected RecurringOccurrence() {}

    public RecurringOccurrence(RecurringRule rule, LocalDate occurrenceDate, Transaction transaction) {
        this.rule = rule;
        this.occurrenceDate = occurrenceDate;
        this.transaction = transaction;
    }
}
