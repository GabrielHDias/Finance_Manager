package io.github.gabrielhdias.financeManager.domain.transaction;

import io.github.gabrielhdias.financeManager.domain.dashboard.CategoryExpenseSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.gabrielhdias.financeManager.domain.dashboard.PeriodTotal;
import io.github.gabrielhdias.financeManager.domain.dashboard.MonthTotal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository
    extends JpaRepository<Transaction, Long> {

    @Query("""
        SELECT new io.github.gabrielhdias.financeManager.domain.dashboard.PeriodTotal(t.date, SUM(t.amount))
        FROM Transaction t
        WHERE t.account.user.id = :userId AND t.type = :type
          AND t.date >= :startDate AND t.date <= :endDate
        GROUP BY t.date ORDER BY t.date
        """)
    List<PeriodTotal> sumByDay(
        @Param("userId") Long userId, @Param("type") TransactionType type,
        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT new io.github.gabrielhdias.financeManager.domain.dashboard.MonthTotal(
            extract(year from t.date), extract(month from t.date), SUM(t.amount))
        FROM Transaction t
        WHERE t.account.user.id = :userId AND t.type = :type
          AND t.date >= :startDate AND t.date <= :endDate
        GROUP BY extract(year from t.date), extract(month from t.date)
        ORDER BY extract(year from t.date), extract(month from t.date)
        """)
    List<MonthTotal> sumByMonth(
        @Param("userId") Long userId, @Param("type") TransactionType type,
        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT t
            FROM Transaction t
            JOIN FETCH t.account
            LEFT JOIN FETCH t.category
            LEFT JOIN FETCH t.destinationAccount
            WHERE t.account.user.id = :userId
            ORDER BY t.date DESC, t.id DESC
            """)
    List<Transaction> findAllByAccountUserId(
        @Param("userId") Long userId
    );

    @Query("""
            SELECT t
            FROM Transaction t
            JOIN FETCH t.account
            LEFT JOIN FETCH t.category
            LEFT JOIN FETCH t.destinationAccount
            WHERE t.id = :id
              AND t.account.user.id = :userId
            """)
    Optional<Transaction> findByIdAndAccountUserId(
        @Param("id") Long id,
        @Param("userId") Long userId
    );

    boolean existsByAccountId(Long accountId);

    boolean existsByDestinationAccountId(Long destinationAccountId);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            SELECT t
            FROM Transaction t
            JOIN FETCH t.account
            LEFT JOIN FETCH t.category
            LEFT JOIN FETCH t.destinationAccount
            WHERE t.account.user.id = :userId
              AND t.date >= COALESCE(:startDate, t.date)
              AND t.date <= COALESCE(:endDate, t.date)
              AND t.type = COALESCE(:type, t.type)
              AND (:accountId IS NULL OR t.account.id = :accountId OR t.destinationAccount.id = :accountId)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
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
            SELECT t
            FROM Transaction t
            JOIN FETCH t.account
            LEFT JOIN FETCH t.category
            LEFT JOIN FETCH t.destinationAccount
            WHERE t.account.user.id = :userId
              AND t.date >= COALESCE(:startDate, t.date)
              AND t.date <= COALESCE(:endDate, t.date)
              AND t.type = COALESCE(:type, t.type)
              AND (:accountId IS NULL OR t.account.id = :accountId OR t.destinationAccount.id = :accountId)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
            ORDER BY t.date DESC, t.id DESC
            """)
    List<Transaction> findLimitedByFilters(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("type") TransactionType type,
        @Param("accountId") Long accountId,
        @Param("categoryId") Long categoryId, Pageable pageable
    );

    @Query(value = """
            SELECT t FROM Transaction t
            JOIN FETCH t.account
            LEFT JOIN FETCH t.category
            LEFT JOIN FETCH t.destinationAccount
            WHERE t.account.user.id = :userId
              AND t.date >= COALESCE(:startDate, t.date)
              AND t.date <= COALESCE(:endDate, t.date)
              AND t.type = COALESCE(:type, t.type)
              AND (:accountId IS NULL OR t.account.id = :accountId OR t.destinationAccount.id = :accountId)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
            ORDER BY t.date DESC, t.id DESC
            """, countQuery = """
            SELECT COUNT(t) FROM Transaction t
            WHERE t.account.user.id = :userId
              AND t.date >= COALESCE(:startDate, t.date)
              AND t.date <= COALESCE(:endDate, t.date)
              AND t.type = COALESCE(:type, t.type)
              AND (:accountId IS NULL OR t.account.id = :accountId OR t.destinationAccount.id = :accountId)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
            """)
    Page<Transaction> findPageByFilters(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("type") TransactionType type,
        @Param("accountId") Long accountId,
        @Param("categoryId") Long categoryId,
        Pageable pageable
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
            SELECT new io.github.gabrielhdias.financeManager.domain.dashboard.CategoryExpenseSummary(
                t.category.id,
                t.category.name,
                SUM(t.amount)
            )
            FROM Transaction t
            WHERE t.account.user.id = :userId
              AND t.type = :expenseType
              AND t.date >= COALESCE(:startDate, t.date)
              AND t.date <= COALESCE(:endDate, t.date)
            GROUP BY t.category.id, t.category.name
            ORDER BY SUM(t.amount) DESC
            """)
    List<CategoryExpenseSummary> sumExpensesByCategory(
        @Param("userId") Long userId,
        @Param("expenseType") TransactionType expenseType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT COALESCE(
                SUM(
                    CASE
                        WHEN t.account.id = :accountId AND t.type = :incomeType THEN t.amount
                        WHEN t.account.id = :accountId AND t.type = io.github.gabrielhdias.financeManager.domain.transaction.TransactionType.EXPENSE THEN -t.amount
                        WHEN t.account.id = :accountId AND t.type = io.github.gabrielhdias.financeManager.domain.transaction.TransactionType.TRANSFER THEN -t.amount
                        WHEN t.destinationAccount.id = :accountId AND t.type = io.github.gabrielhdias.financeManager.domain.transaction.TransactionType.TRANSFER THEN t.amount
                        ELSE 0
                    END
                ),
                0
            )
            FROM Transaction t
            WHERE t.account.id = :accountId OR t.destinationAccount.id = :accountId
            """)
    BigDecimal calculateBalanceByAccountId(
        @Param("accountId") Long accountId,
        @Param("incomeType") TransactionType incomeType
    );
    @Query("""
        SELECT new io.github.gabrielhdias.financeManager.api.account.dto.AccountBalanceResponse(
            a.id, a.openingBalance + COALESCE(SUM(CASE
                WHEN t.type = :incomeType AND t.account = a THEN t.amount
                WHEN t.type = io.github.gabrielhdias.financeManager.domain.transaction.TransactionType.EXPENSE AND t.account = a THEN -t.amount
                WHEN t.type = io.github.gabrielhdias.financeManager.domain.transaction.TransactionType.TRANSFER AND t.account = a THEN -t.amount
                WHEN t.type = io.github.gabrielhdias.financeManager.domain.transaction.TransactionType.TRANSFER AND t.destinationAccount = a THEN t.amount
                ELSE 0 END), 0))
        FROM Account a LEFT JOIN Transaction t ON t.account = a OR t.destinationAccount = a
        WHERE a.user.id = :userId GROUP BY a.id, a.openingBalance ORDER BY a.id
        """)
    List<io.github.gabrielhdias.financeManager.api.account.dto.AccountBalanceResponse> balancesByUser(
        @Param("userId") Long userId, @Param("incomeType") TransactionType incomeType);
}
