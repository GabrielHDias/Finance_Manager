package io.github.gabrielhdias.financeManager.domain.recurring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringRuleRepository extends JpaRepository<RecurringRule, Long> {
    List<RecurringRule> findByAccountUserIdAndActiveTrue(Long userId);

    java.util.Optional<RecurringRule> findByIdAndAccountUserId(Long id, Long userId);
}
