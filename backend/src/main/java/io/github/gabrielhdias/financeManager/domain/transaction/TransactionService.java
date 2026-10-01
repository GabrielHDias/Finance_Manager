package io.github.gabrielhdias.financeManager.domain.transaction;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.account.AccountRepository;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryRepository;
import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(
        TransactionRepository transactionRepository,
        AccountRepository accountRepository,
        CategoryRepository categoryRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Transaction create(
        Long userId,
        String description,
        BigDecimal amount,
        LocalDate date,
        TransactionType type,
        Long accountId,
        Long categoryId
    ) {
        Account account = findAccountByIdAndUserId(
            accountId,
            userId
        );

        Category category = findCategoryByIdAndUserId(
            categoryId,
            userId
        );

        Transaction transaction = new Transaction(
            description,
            amount,
            date,
            type,
            account,
            category
        );

        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<Transaction> listByUser(Long userId) {
        return transactionRepository.findAllByAccountUserId(
            userId
        );
    }

    @Transactional(readOnly = true)
    public List<Transaction> filterByUser(
        Long userId,
        LocalDate startDate,
        LocalDate endDate,
        TransactionType type,
        Long accountId,
        Long categoryId
    ) {
        validatePeriod(
            startDate,
            endDate
        );

        return transactionRepository.findAllByFilters(
            userId,
            startDate,
            endDate,
            type,
            accountId,
            categoryId
        );
    }

    @Transactional(readOnly = true)
    public Transaction findById(
        Long userId,
        Long transactionId
    ) {
        return findTransactionByIdAndUserId(
            transactionId,
            userId
        );
    }

    @Transactional
    public Transaction update(
        Long userId,
        Long transactionId,
        String description,
        BigDecimal amount,
        LocalDate date,
        TransactionType type,
        Long accountId,
        Long categoryId
    ) {
        Transaction transaction =
            findTransactionByIdAndUserId(
                transactionId,
                userId
            );

        Account account = findAccountByIdAndUserId(
            accountId,
            userId
        );

        Category category = findCategoryByIdAndUserId(
            categoryId,
            userId
        );

        transaction.updateDescription(description);
        transaction.updateAmount(amount);
        transaction.updateDate(date);
        transaction.updateType(type);
        transaction.updateAccount(account);
        transaction.updateCategory(category);

        return transaction;
    }

    @Transactional
    public void delete(
        Long userId,
        Long transactionId
    ) {
        Transaction transaction =
            findTransactionByIdAndUserId(
                transactionId,
                userId
            );

        transactionRepository.delete(transaction);
    }

    private void validatePeriod(
        LocalDate startDate,
        LocalDate endDate
    ) {
        if (startDate != null
            && endDate != null
            && startDate.isAfter(endDate)) {
            throw new BusinessRuleException(
                "A data inicial não pode ser posterior à data final"
            );
        }
    }

    private Transaction findTransactionByIdAndUserId(
        Long transactionId,
        Long userId
    ) {
        return transactionRepository
            .findByIdAndAccountUserId(
                transactionId,
                userId
            )
            .orElseThrow(
                () -> new ResourceNotFoundException(
                    "Transação não encontrada"
                )
            );
    }

    private Account findAccountByIdAndUserId(
        Long accountId,
        Long userId
    ) {
        return accountRepository
            .findByIdAndUserId(
                accountId,
                userId
            )
            .orElseThrow(
                () -> new ResourceNotFoundException(
                    "Conta não encontrada"
                )
            );
    }

    private Category findCategoryByIdAndUserId(
        Long categoryId,
        Long userId
    ) {
        return categoryRepository
            .findByIdAndUserId(
                categoryId,
                userId
            )
            .orElseThrow(
                () -> new ResourceNotFoundException(
                    "Categoria não encontrada"
                )
            );
    }
}
