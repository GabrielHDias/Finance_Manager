package io.github.gabrielhdias.financeManager.domain.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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
