package io.github.gabrielhdias.financeManager.api.account;

import io.github.gabrielhdias.financeManager.api.account.dto.AccountRequest;
import io.github.gabrielhdias.financeManager.api.account.dto.AccountResponse;
import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.account.AccountService;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AuthenticatedUser authenticatedUser;

    @InjectMocks
    private AccountController accountController;

    @Test
    void shouldCreateAccount() {
        Long userId = 1L;

        AccountRequest request =
            new AccountRequest("Nubank");

        Account account = mock(Account.class);

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(account.getId())
            .thenReturn(10L);

        when(account.getName())
            .thenReturn("Nubank");

        when(accountService.create(
            userId,
            "Nubank"
        )).thenReturn(account);

        ResponseEntity<AccountResponse> response =
            accountController.create(request);

        assertEquals(
            HttpStatus.CREATED,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            10L,
            response.getBody().id()
        );

        assertEquals(
            "Nubank",
            response.getBody().name()
        );

        verify(authenticatedUser)
            .getId();

        verify(accountService).create(
            userId,
            "Nubank"
        );
    }

    @Test
    void shouldListAccounts() {
        Long userId = 1L;

        Account firstAccount = mock(Account.class);
        Account secondAccount = mock(Account.class);

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(firstAccount.getId())
            .thenReturn(10L);

        when(firstAccount.getName())
            .thenReturn("Nubank");

        when(secondAccount.getId())
            .thenReturn(20L);

        when(secondAccount.getName())
            .thenReturn("Carteira");

        when(accountService.listByUser(userId))
            .thenReturn(List.of(
                firstAccount,
                secondAccount
            ));

        ResponseEntity<List<AccountResponse>> response =
            accountController.list();

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());

        assertEquals(
            "Nubank",
            response.getBody().get(0).name()
        );

        assertEquals(
            "Carteira",
            response.getBody().get(1).name()
        );

        verify(authenticatedUser)
            .getId();

        verify(accountService)
            .listByUser(userId);
    }

    @Test
    void shouldFindAccountById() {
        Long userId = 1L;
        Long accountId = 10L;

        Account account = mock(Account.class);

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(account.getId())
            .thenReturn(accountId);

        when(account.getName())
            .thenReturn("Nubank");

        when(accountService.findById(
            userId,
            accountId
        )).thenReturn(account);

        ResponseEntity<AccountResponse> response =
            accountController.findById(accountId);

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            accountId,
            response.getBody().id()
        );

        assertEquals(
            "Nubank",
            response.getBody().name()
        );

        verify(accountService).findById(
            userId,
            accountId
        );
    }

    @Test
    void shouldRenameAccount() {
        Long userId = 1L;
        Long accountId = 10L;

        AccountRequest request =
            new AccountRequest("Conta Principal");

        Account account = mock(Account.class);

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(account.getId())
            .thenReturn(accountId);

        when(account.getName())
            .thenReturn("Conta Principal");

        when(accountService.rename(
            userId,
            accountId,
            "Conta Principal"
        )).thenReturn(account);

        ResponseEntity<AccountResponse> response =
            accountController.rename(
                accountId,
                request
            );

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            accountId,
            response.getBody().id()
        );

        assertEquals(
            "Conta Principal",
            response.getBody().name()
        );

        verify(accountService).rename(
            userId,
            accountId,
            "Conta Principal"
        );
    }

    @Test
    void shouldDeleteAccount() {
        Long userId = 1L;
        Long accountId = 10L;

        when(authenticatedUser.getId())
            .thenReturn(userId);

        ResponseEntity<Void> response =
            accountController.delete(accountId);

        assertEquals(
            HttpStatus.NO_CONTENT,
            response.getStatusCode()
        );

        assertNull(response.getBody());

        verify(accountService).delete(
            userId,
            accountId
        );
    }
}
