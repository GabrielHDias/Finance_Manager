package io.github.gabrielhdias.financeManager.api.transaction;

import io.github.gabrielhdias.financeManager.api.transaction.dto.TransactionRequest;
import io.github.gabrielhdias.financeManager.api.transaction.dto.TransactionResponse;
import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.transaction.Transaction;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionService;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private AuthenticatedUser authenticatedUser;

    @InjectMocks
    private TransactionController transactionController;

    @Test
    void shouldCreateTransaction() {
        Long userId = 1L;

        TransactionRequest request = new TransactionRequest(
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            10L,
            20L
        );

        Transaction transaction = createMockTransaction();

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(transactionService.create(
            userId,
            request.description(),
            request.amount(),
            request.date(),
            request.type(),
            request.accountId(),
            request.categoryId()
        )).thenReturn(transaction);

        ResponseEntity<TransactionResponse> response =
            transactionController.create(request);

        assertEquals(
            HttpStatus.CREATED,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            "Mercado",
            response.getBody().description()
        );

        verify(transactionService).create(
            userId,
            request.description(),
            request.amount(),
            request.date(),
            request.type(),
            request.accountId(),
            request.categoryId()
        );
    }

    @Test
    void shouldListTransactionsWithoutFilters() {
        Long userId = 1L;

        Transaction transaction = createMockTransaction();

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(transactionService.filterByUser(
            userId,
            null,
            null,
            null,
            null,
            null
        )).thenReturn(
            List.of(transaction)
        );

        ResponseEntity<List<TransactionResponse>> response =
            transactionController.list(
                null,
                null,
                null,
                null,
                null
            );

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(transactionService)
            .filterByUser(
                userId,
                null,
                null,
                null,
                null,
                null
            );
    }

    @Test
    void shouldListTransactionsWithFilters() {
        Long userId = 1L;

        LocalDate startDate =
            LocalDate.of(2026, 10, 1);

        LocalDate endDate =
            LocalDate.of(2026, 10, 31);

        Transaction transaction = createMockTransaction();

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(transactionService.filterByUser(
            userId,
            startDate,
            endDate,
            TransactionType.EXPENSE,
            10L,
            20L
        )).thenReturn(
            List.of(transaction)
        );

        ResponseEntity<List<TransactionResponse>> response =
            transactionController.list(
                startDate,
                endDate,
                TransactionType.EXPENSE,
                10L,
                20L
            );

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(transactionService)
            .filterByUser(
                userId,
                startDate,
                endDate,
                TransactionType.EXPENSE,
                10L,
                20L
            );
    }

    @Test
    void shouldFindTransactionById() {
        Long userId = 1L;
        Long transactionId = 100L;

        Transaction transaction = createMockTransaction();

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(transactionService.findById(
            userId,
            transactionId
        )).thenReturn(transaction);

        ResponseEntity<TransactionResponse> response =
            transactionController.findById(
                transactionId
            );

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        verify(transactionService)
            .findById(
                userId,
                transactionId
            );
    }

    @Test
    void shouldUpdateTransaction() {
        Long userId = 1L;
        Long transactionId = 100L;

        TransactionRequest request = new TransactionRequest(
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            10L,
            20L
        );

        Transaction transaction = createMockTransaction();

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(transactionService.update(
            userId,
            transactionId,
            request.description(),
            request.amount(),
            request.date(),
            request.type(),
            request.accountId(),
            request.categoryId()
        )).thenReturn(transaction);

        ResponseEntity<TransactionResponse> response =
            transactionController.update(
                transactionId,
                request
            );

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        verify(transactionService)
            .update(
                userId,
                transactionId,
                request.description(),
                request.amount(),
                request.date(),
                request.type(),
                request.accountId(),
                request.categoryId()
            );
    }

    @Test
    void shouldDeleteTransaction() {
        Long userId = 1L;
        Long transactionId = 100L;

        when(authenticatedUser.getId())
            .thenReturn(userId);

        ResponseEntity<Void> response =
            transactionController.delete(
                transactionId
            );

        assertEquals(
            HttpStatus.NO_CONTENT,
            response.getStatusCode()
        );

        assertNull(response.getBody());

        verify(transactionService)
            .delete(
                userId,
                transactionId
            );
    }

    private Transaction createMockTransaction() {
        Account account = mock(Account.class);
        Category category = mock(Category.class);
        Transaction transaction = mock(Transaction.class);

        when(account.getId())
            .thenReturn(10L);

        when(account.getName())
            .thenReturn("Nubank");

        when(category.getId())
            .thenReturn(20L);

        when(category.getName())
            .thenReturn("Alimentação");

        when(transaction.getId())
            .thenReturn(100L);

        when(transaction.getDescription())
            .thenReturn("Mercado");

        when(transaction.getAmount())
            .thenReturn(new BigDecimal("150.00"));

        when(transaction.getDate())
            .thenReturn(LocalDate.of(2026, 10, 1));

        when(transaction.getType())
            .thenReturn(TransactionType.EXPENSE);

        when(transaction.getAccount())
            .thenReturn(account);

        when(transaction.getCategory())
            .thenReturn(category);

        return transaction;
    }
}
