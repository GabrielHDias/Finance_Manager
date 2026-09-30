package io.github.gabrielhdias.financeManager.domain.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByAccountUserId(Long userId);

    Optional<Transaction> findByIdAndAccountUserId(Long id, Long userId);

    boolean existsByAccountId(Long accountId);

    boolean existsByCategoryId(Long categoryId);
}
