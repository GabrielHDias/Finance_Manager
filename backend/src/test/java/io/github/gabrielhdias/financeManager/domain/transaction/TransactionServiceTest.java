package io.github.gabrielhdias.financeManager.domain.transaction;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.account.AccountRepository;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryRepository;
import io.github.gabrielhdias.financeManager.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void shouldCreateTransactionWhenAccountAndCategoryBelongToUser() {
        Long userId = 1L;
        Long accountId = 10L;
        Long categoryId = 20L;

        User user = createUser();
        Account account = new Account("Nubank", user);
        Category category = new Category("Alimentação", user);

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(
            categoryId,
            userId
        )).thenReturn(Optional.of(category));

        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.create(
            userId,
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 9, 30),
            TransactionType.EXPENSE,
            accountId,
            categoryId
        );

        assertNotNull(result);
        assertEquals("Mercado", result.getDescription());
        assertEquals(new BigDecimal("150.00"), result.getAmount());
        assertEquals(TransactionType.EXPENSE, result.getType());
        assertEquals(account, result.getAccount());
        assertEquals(category, result.getCategory());

        verify(transactionRepository)
            .save(any(Transaction.class));
    }

    @Test
    void shouldNotCreateTransactionWhenAccountDoesNotBelongToUser() {
        Long userId = 1L;
        Long accountId = 10L;
        Long categoryId = 20L;

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.create(
                userId,
                "Mercado",
                new BigDecimal("150.00"),
                LocalDate.of(2026, 9, 30),
                TransactionType.EXPENSE,
                accountId,
                categoryId
            )
        );

        assertEquals(
            "Conta não encontrada",
            exception.getMessage()
        );

        verify(categoryRepository, never())
            .findByIdAndUserId(anyLong(), anyLong());

        verify(transactionRepository, never())
            .save(any(Transaction.class));
    }

    @Test
    void shouldNotCreateTransactionWhenCategoryDoesNotBelongToUser() {
        Long userId = 1L;
        Long accountId = 10L;
        Long categoryId = 20L;

        User user = createUser();
        Account account = new Account("Nubank", user);

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(
            categoryId,
            userId
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.create(
                userId,
                "Mercado",
                new BigDecimal("150.00"),
                LocalDate.of(2026, 9, 30),
                TransactionType.EXPENSE,
                accountId,
                categoryId
            )
        );

        assertEquals(
            "Categoria não encontrada",
            exception.getMessage()
        );

        verify(transactionRepository, never())
            .save(any(Transaction.class));
    }

    @Test
    void shouldListTransactionsByUser() {
        Long userId = 1L;

        User user = createUser();
        Account account = new Account("Nubank", user);
        Category category = new Category("Alimentação", user);

        List<Transaction> transactions = List.of(
            new Transaction(
                "Mercado",
                new BigDecimal("150.00"),
                LocalDate.of(2026, 9, 30),
                TransactionType.EXPENSE,
                account,
                category
            ),
            new Transaction(
                "Salário",
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 9, 30),
                TransactionType.INCOME,
                account,
                category
            )
        );

        when(transactionRepository.findAllByAccountUserId(userId))
            .thenReturn(transactions);

        List<Transaction> result =
            transactionService.listByUser(userId);

        assertEquals(2, result.size());
        assertEquals(transactions, result);

        verify(transactionRepository)
            .findAllByAccountUserId(userId);
    }

    @Test
    void shouldFindTransactionByIdAndUser() {
        Long userId = 1L;
        Long transactionId = 100L;

        Transaction transaction = createTransaction();

        when(transactionRepository.findByIdAndAccountUserId(
            transactionId,
            userId
        )).thenReturn(Optional.of(transaction));

        Transaction result = transactionService.findById(
            userId,
            transactionId
        );

        assertEquals(transaction, result);
    }

    @Test
    void shouldNotFindTransactionWhenTransactionDoesNotBelongToUser() {
        Long userId = 1L;
        Long transactionId = 100L;

        when(transactionRepository.findByIdAndAccountUserId(
            transactionId,
            userId
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.findById(
                userId,
                transactionId
            )
        );

        assertEquals(
            "Transação não encontrada",
            exception.getMessage()
        );
    }

    @Test
    void shouldUpdateTransaction() {
        Long userId = 1L;
        Long transactionId = 100L;
        Long accountId = 10L;
        Long categoryId = 20L;

        User user = createUser();

        Account oldAccount = new Account("Nubank", user);
        Category oldCategory = new Category("Alimentação", user);

        Transaction transaction = new Transaction(
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 9, 30),
            TransactionType.EXPENSE,
            oldAccount,
            oldCategory
        );

        Account newAccount = new Account("Carteira", user);
        Category newCategory = new Category("Transporte", user);

        when(transactionRepository.findByIdAndAccountUserId(
            transactionId,
            userId
        )).thenReturn(Optional.of(transaction));

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(newAccount));

        when(categoryRepository.findByIdAndUserId(
            categoryId,
            userId
        )).thenReturn(Optional.of(newCategory));

        Transaction result = transactionService.update(
            userId,
            transactionId,
            "Uber",
            new BigDecimal("35.90"),
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            accountId,
            categoryId
        );

        assertEquals("Uber", result.getDescription());
        assertEquals(new BigDecimal("35.90"), result.getAmount());
        assertEquals(LocalDate.of(2026, 10, 1), result.getDate());
        assertEquals(TransactionType.EXPENSE, result.getType());
        assertEquals(newAccount, result.getAccount());
        assertEquals(newCategory, result.getCategory());

        verify(transactionRepository, never())
            .save(any(Transaction.class));
    }

    @Test
    void shouldNotUpdateTransactionWhenNewAccountDoesNotBelongToUser() {
        Long userId = 1L;
        Long transactionId = 100L;
        Long accountId = 10L;
        Long categoryId = 20L;

        Transaction transaction = createTransaction();

        when(transactionRepository.findByIdAndAccountUserId(
            transactionId,
            userId
        )).thenReturn(Optional.of(transaction));

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.update(
                userId,
                transactionId,
                "Uber",
                new BigDecimal("35.90"),
                LocalDate.of(2026, 10, 1),
                TransactionType.EXPENSE,
                accountId,
                categoryId
            )
        );

        assertEquals(
            "Conta não encontrada",
            exception.getMessage()
        );

        verify(categoryRepository, never())
            .findByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void shouldNotUpdateTransactionWhenNewCategoryDoesNotBelongToUser() {
        Long userId = 1L;
        Long transactionId = 100L;
        Long accountId = 10L;
        Long categoryId = 20L;

        User user = createUser();
        Transaction transaction = createTransaction();
        Account account = new Account("Carteira", user);

        when(transactionRepository.findByIdAndAccountUserId(
            transactionId,
            userId
        )).thenReturn(Optional.of(transaction));

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(
            categoryId,
            userId
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.update(
                userId,
                transactionId,
                "Uber",
                new BigDecimal("35.90"),
                LocalDate.of(2026, 10, 1),
                TransactionType.EXPENSE,
                accountId,
                categoryId
            )
        );

        assertEquals(
            "Categoria não encontrada",
            exception.getMessage()
        );
    }

    @Test
    void shouldDeleteTransactionWhenItBelongsToUser() {
        Long userId = 1L;
        Long transactionId = 100L;

        Transaction transaction = createTransaction();

        when(transactionRepository.findByIdAndAccountUserId(
            transactionId,
            userId
        )).thenReturn(Optional.of(transaction));

        transactionService.delete(
            userId,
            transactionId
        );

        verify(transactionRepository)
            .delete(transaction);
    }

    @Test
    void shouldNotDeleteTransactionWhenItDoesNotBelongToUser() {
        Long userId = 1L;
        Long transactionId = 100L;

        when(transactionRepository.findByIdAndAccountUserId(
            transactionId,
            userId
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.delete(
                userId,
                transactionId
            )
        );

        assertEquals(
            "Transação não encontrada",
            exception.getMessage()
        );

        verify(transactionRepository, never())
            .delete(any(Transaction.class));
    }

    private User createUser() {
        return new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );
    }

    private Transaction createTransaction() {
        User user = createUser();

        Account account = new Account(
            "Nubank",
            user
        );

        Category category = new Category(
            "Alimentação",
            user
        );

        return new Transaction(
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 9, 30),
            TransactionType.EXPENSE,
            account,
            category
        );
    }
}
