package io.github.gabrielhdias.financeManager.domain.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository
    extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByAccountUserId(Long userId);

    Optional<Transaction> findByIdAndAccountUserId(
        Long id,
        Long userId
    );

    boolean existsByAccountId(Long accountId);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            SELECT t
            FROM Transaction t
            WHERE t.account.user.id = :userId
              AND t.date >= COALESCE(:startDate, t.date)
              AND t.date <= COALESCE(:endDate, t.date)
              AND t.type = COALESCE(:type, t.type)
              AND t.account.id = COALESCE(:accountId, t.account.id)
              AND t.category.id = COALESCE(:categoryId, t.category.id)
            ORDER BY t.date DESC, t.id DESC
            """)
    List<Transaction> findAllByFilters(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("type") TransactionType type,
        @Param("accountId") Long accountId,
        @Param("categoryId") Long categoryId
    );

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.account.user.id = :userId
              AND t.type = :type
              AND t.date >= COALESCE(:startDate, t.date)
              AND t.date <= COALESCE(:endDate, t.date)
            """)
    BigDecimal sumAmountByUserAndTypeAndPeriod(
        @Param("userId") Long userId,
        @Param("type") TransactionType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT COALESCE(
                SUM(
                    CASE
                        WHEN t.type = :incomeType
                            THEN t.amount
                        ELSE -t.amount
                    END
                ),
                0
            )
            FROM Transaction t
            WHERE t.account.id = :accountId
            """)
    BigDecimal calculateBalanceByAccountId(
        @Param("accountId") Long accountId,
        @Param("incomeType") TransactionType incomeType
    );
}
