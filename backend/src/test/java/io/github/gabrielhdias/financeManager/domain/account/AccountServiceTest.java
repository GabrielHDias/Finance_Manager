package io.github.gabrielhdias.financeManager.domain.account;

import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
            "password"
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

        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void shouldNotCreateAccountWhenNameAlreadyExists() {
        Long userId = 1L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

        when(accountRepository.existsByUserIdAndName(
            userId,
            "Nubank"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> accountService.create(userId, "Nubank")
        );

        assertEquals(
            "Já existe uma conta com esse nome para o usuário",
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
            "password"
        );

        List<Account> accounts = List.of(
            new Account("Nubank", user),
            new Account("Carteira", user)
        );

        when(accountRepository.findAllByUserId(userId))
            .thenReturn(accounts);

        List<Account> result = accountService.listByUser(userId);

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
            "password"
        );

        Account account = new Account("Nubank", user);

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
            .findByIdAndUserId(accountId, userId);
    }

    @Test
    void shouldRenameAccount() {
        Long userId = 1L;
        Long accountId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        Account account = new Account("Nubank", user);

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(accountRepository.existsByUserIdAndName(
            userId,
            "Nubank Principal"
        )).thenReturn(false);

        Account result = accountService.rename(
            userId,
            accountId,
            "Nubank Principal"
        );

        assertEquals(
            "Nubank Principal",
            result.getName()
        );
    }

    @Test
    void shouldDeleteAccountWithoutTransactions() {
        Long userId = 1L;
        Long accountId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        Account account = new Account("Nubank", user);

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(transactionRepository.existsByAccountId(accountId))
            .thenReturn(false);

        accountService.delete(userId, accountId);

        verify(accountRepository).delete(account);
    }

    @Test
    void shouldNotDeleteAccountWithTransactions() {
        Long userId = 1L;
        Long accountId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        Account account = new Account("Nubank", user);

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(transactionRepository.existsByAccountId(accountId))
            .thenReturn(true);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> accountService.delete(userId, accountId)
        );

        assertEquals(
            "Não é possível excluir uma conta que possui transações",
            exception.getMessage()
        );

        verify(accountRepository, never())
            .delete(any(Account.class));
    }
    @Test
    void shouldNotCreateAccountWhenUserDoesNotExist() {
        Long userId = 1L;

        when(userRepository.findById(userId))
            .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> accountService.create(userId, "Nubank")
        );

        assertEquals(
            "Usuário não encontrado",
            exception.getMessage()
        );

        verify(accountRepository, never())
            .save(any(Account.class));
    }
    @Test
    void shouldNotFindAccountWhenAccountDoesNotExistForUser() {
        Long userId = 1L;
        Long accountId = 10L;

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> accountService.findById(userId, accountId)
        );

        assertEquals(
            "Conta não encontrada",
            exception.getMessage()
        );
    }
    @Test
    void shouldNotRenameAccountWhenNameAlreadyExists() {
        Long userId = 1L;
        Long accountId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        Account account = new Account(
            "Nubank",
            user
        );

        when(accountRepository.findByIdAndUserId(
            accountId,
            userId
        )).thenReturn(Optional.of(account));

        when(accountRepository.existsByUserIdAndName(
            userId,
            "Carteira"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
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
}
