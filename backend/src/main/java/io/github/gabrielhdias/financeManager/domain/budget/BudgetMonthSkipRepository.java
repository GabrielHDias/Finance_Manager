package io.github.gabrielhdias.financeManager.domain.budget;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetMonthSkipRepository extends JpaRepository<BudgetMonthSkip, BudgetMonthSkip.Key> {
}
