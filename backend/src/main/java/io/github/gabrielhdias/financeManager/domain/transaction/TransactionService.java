package io.github.gabrielhdias.financeManager.domain.transaction;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.account.AccountRepository;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryRepository;
import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
        return create(userId, description, amount, date, type, accountId, categoryId, null);
    }

    @Transactional
    public Transaction create(
        Long userId,
        String description,
        BigDecimal amount,
        LocalDate date,
        TransactionType type,
        Long accountId,
        Long categoryId,
        Long destinationAccountId
    ) {
        Account account = findAccountByIdAndUserId(accountId, userId);
        if (type == TransactionType.TRANSFER) {
            return transactionRepository.save(new Transaction(
                description, amount, date, account, requireDestination(userId, account, destinationAccountId, categoryId)
            ));
        }
        Category category = requireMatchingCategory(userId, type, categoryId);
        return transactionRepository.save(new Transaction(description, amount, date, type, account, category));
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

        List<Transaction> result = transactionRepository.findLimitedByFilters(
            userId, startDate, endDate, type, accountId, categoryId, PageRequest.of(0, 10001));
        if (result.size() > 10000) throw new BusinessRuleException(
            "Exportação limitada a 10000 transações. Reduza o período ou aplique filtros.");
        return result;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> pageByUser(
        Long userId, LocalDate startDate, LocalDate endDate,
        TransactionType type, Long accountId, Long categoryId, int page, int size
    ) {
        validatePeriod(startDate, endDate);
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("A página deve ser maior ou igual a zero e o tamanho deve estar entre 1 e 100.");
        }
        return transactionRepository.findPageByFilters(
            userId, startDate, endDate, type, accountId, categoryId, PageRequest.of(page, size)
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
        return update(userId, transactionId, description, amount, date, type, accountId, categoryId, null);
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
        Long categoryId,
        Long destinationAccountId
    ) {
        Transaction transaction = findTransactionByIdAndUserId(transactionId, userId);
        Account account = findAccountByIdAndUserId(accountId, userId);
        transaction.updateDescription(description);
        transaction.updateAmount(amount);
        transaction.updateDate(date);
        if (type == TransactionType.TRANSFER) {
            transaction.updateAccount(account);
            transaction.becomeTransfer(requireDestination(userId, account, destinationAccountId, categoryId));
            return transaction;
        }
        transaction.becomeMovement(type, requireMatchingCategory(userId, type, categoryId));
        transaction.updateAccount(account);
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

    private Category requireMatchingCategory(Long userId, TransactionType type, Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Informe a categoria");
        }
        Category category = findCategoryByIdAndUserId(categoryId, userId);
        if (!category.getType().name().equals(type.name())) {
            throw new BusinessRuleException("A categoria precisa ser do mesmo tipo da transação");
        }
        return category;
    }

    private Account requireDestination(Long userId, Account source, Long destinationAccountId, Long categoryId) {
        if (categoryId != null) {
            throw new IllegalArgumentException("Transferência não usa categoria");
        }
        if (destinationAccountId == null) {
            throw new IllegalArgumentException("Informe a conta de destino");
        }
        Account destination = findAccountByIdAndUserId(destinationAccountId, userId);
        if (source.getId().equals(destination.getId())) {
            throw new BusinessRuleException("A conta de origem e a de destino precisam ser diferentes");
        }
        return destination;
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
