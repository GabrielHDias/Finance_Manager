package io.github.gabrielhdias.financeManager.domain.account;

import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.exception.DuplicateResourceException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldCreateAccountWhenNameIsAvailable() {
        Long userId = 1L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "$2a$10$encodedPassword"
        );

        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

        when(accountRepository.existsByUserIdAndName(
            userId,
            "Nubank"
        )).thenReturn(false);

        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.create(
            userId,
            "Nubank"
        );

        assertNotNull(result);
        assertEquals("Nubank", result.getName());
        assertEquals(user, result.getUser());

        verify(accountRepository)
            .save(any(Account.class));
    }

    @Test
    void shouldNotCreateAccountWhenNameAlreadyExists() {
        Long userId = 1L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "$2a$10$encodedPassword"
        );

        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

        when(accountRepository.existsByUserIdAndName(
            userId,
            "Nubank"
        )).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> accountService.create(
                userId,
                "Nubank"
            )
        );

        assertEquals(
            "Já existe uma conta com esse nome para o usuário",
            exception.getMessage()
        );

        verify(accountRepository, never())
            .save(any(Account.class));
    }

    @Test
    void shouldNotCreateAccountWhenUserDoesNotExist() {
        Long userId = 1L;

        when(userRepository.findById(userId))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> accountService.create(
                userId,
                "Nubank"
            )
        );

        assertEquals(
            "Usuário não encontrado",
            exception.getMessage()
        );

        verify(accountRepository, never())
            .save(any(Account.class));
    }

    @Test
    void shouldListAccountsByUser() {
        Long userId = 1L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "$2a$10$encodedPassword"
        );

        List<Account> accounts = List.of(
            new Account("Nubank", user),
            new Account("Carteira", user)
        );

        when(accountRepository.findAllByUserId(userId))
            .thenReturn(accounts);

        List<Account> result =
            accountService.listByUser(userId);

        assertEquals(2, result.size());
        assertEquals(accounts, result);

        verify(accountRepository)
            .findAllByUserId(userId);
    }

    @Test
    void shouldFindAccountByIdAndUser() {
        Long userId = 1L;
        Long accountId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "$2a$10$encodedPassword"
        );

        Account account =
            new Account("Nubank", user);

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        Account result = accountService.findById(
            userId,
            accountId
        );

        assertEquals(account, result);

        verify(accountRepository)
            .findByIdAndUserId(
                accountId,
                userId
            );
    }

    @Test
    void shouldNotFindAccountWhenAccountDoesNotExistForUser() {
        Long userId = 1L;
        Long accountId = 10L;

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> accountService.findById(
                userId,
                accountId
            )
        );

        assertEquals(
            "Conta não encontrada",
            exception.getMessage()
        );
    }

    @Test
    void shouldCalculateAccountBalance() {
        Long userId = 1L;
        Long accountId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "$2a$10$encodedPassword"
        );

        Account account =
            new Account("Nubank", user);

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(transactionRepository.calculateBalanceByAccountId(
            accountId,
            TransactionType.INCOME
        )).thenReturn(
            new BigDecimal("850.00")
        );

        BigDecimal result =
            accountService.calculateBalance(
                userId,
                accountId
            );

        assertEquals(
            new BigDecimal("850.00"),
            result
        );

        verify(accountRepository)
            .findByIdAndUserId(
                accountId,
                userId
            );

        verify(transactionRepository)
            .calculateBalanceByAccountId(
                accountId,
                TransactionType.INCOME
            );
    }

    @Test
    void shouldNotCalculateBalanceWhenAccountDoesNotBelongToUser() {
        Long userId = 1L;
        Long accountId = 10L;

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> accountService.calculateBalance(
                userId,
                accountId
            )
        );

        assertEquals(
            "Conta não encontrada",
            exception.getMessage()
        );

        verify(transactionRepository, never())
            .calculateBalanceByAccountId(
                anyLong(),
                any(TransactionType.class)
            );
    }

    @Test
    void shouldRenameAccount() {
        Long userId = 1L;
        Long accountId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "$2a$10$encodedPassword"
        );

        Account account =
            new Account("Nubank", user);

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(accountRepository.existsByUserIdAndName(
            userId,
            "Conta Principal"
        )).thenReturn(false);

        Account result = accountService.rename(
            userId,
            accountId,
            "Conta Principal"
        );

        assertEquals(
            "Conta Principal",
            result.getName()
        );
    }

    @Test
    void shouldNotRenameAccountWhenNameAlreadyExists() {
        Long userId = 1L;
        Long accountId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "$2a$10$encodedPassword"
        );

        Account account =
            new Account("Nubank", user);

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(accountRepository.existsByUserIdAndName(
            userId,
            "Carteira"
        )).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> accountService.rename(
                userId,
                accountId,
                "Carteira"
            )
        );

        assertEquals(
            "Já existe uma conta com esse nome para o usuário",
            exception.getMessage()
        );

        assertEquals(
            "Nubank",
            account.getName()
        );
    }

    @Test
    void shouldDeleteAccountWithoutTransactions() {
        Long userId = 1L;
        Long accountId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "$2a$10$encodedPassword"
        );

        Account account =
            new Account("Nubank", user);

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(transactionRepository.existsByAccountId(accountId))
            .thenReturn(false);

        accountService.delete(
            userId,
            accountId
        );

        verify(accountRepository)
            .delete(account);
    }

    @Test
    void shouldNotDeleteAccountWithTransactions() {
        Long userId = 1L;
        Long accountId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "$2a$10$encodedPassword"
        );

        Account account =
            new Account("Nubank", user);

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(transactionRepository.existsByAccountId(accountId))
            .thenReturn(true);

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> accountService.delete(
                userId,
                accountId
            )
        );

        assertEquals(
            "Não é possível excluir uma conta que possui transações",
            exception.getMessage()
        );

        verify(accountRepository, never())
            .delete(any(Account.class));
    }
}
