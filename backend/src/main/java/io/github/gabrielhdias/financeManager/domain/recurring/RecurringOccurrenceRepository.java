package io.github.gabrielhdias.financeManager.domain.recurring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface RecurringOccurrenceRepository extends JpaRepository<RecurringOccurrence, Long> {
    boolean existsByRuleIdAndOccurrenceDate(Long ruleId, LocalDate occurrenceDate);
}
