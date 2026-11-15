package io.github.gabrielhdias.financeManager.domain.budget;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    @Query("SELECT b FROM Budget b JOIN FETCH b.category c WHERE c.user.id = :userId AND b.periodStart <= :until")
    List<Budget> findUpTo(@Param("userId") Long userId, @Param("until") LocalDate until);

    @Query("SELECT b FROM Budget b JOIN FETCH b.category c WHERE c.user.id = :userId AND b.periodStart = :period ORDER BY c.name, b.id")
    List<Budget> findForMonth(@Param("userId") Long userId, @Param("period") LocalDate period);

    Optional<Budget> findByIdAndCategoryUserId(Long id, Long userId);
    boolean existsByCategoryId(Long categoryId);

    // PostgreSQL resolves concurrent saves of the same category/month atomically.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO monthly_budgets (category_id, period_start, limit_amount, repeats_monthly)
        VALUES (:categoryId, :period, :amount, :repeats)
        ON CONFLICT (category_id, period_start) DO UPDATE
            SET limit_amount = EXCLUDED.limit_amount,
                repeats_monthly = EXCLUDED.repeats_monthly
        """, nativeQuery = true)
    void saveLimit(@Param("categoryId") Long categoryId, @Param("period") LocalDate period,
                   @Param("amount") BigDecimal amount, @Param("repeats") boolean repeats);
}
