package io.github.gabrielhdias.financeManager.api.transaction;

import io.github.gabrielhdias.financeManager.api.transaction.dto.TransactionRequest;
import io.github.gabrielhdias.financeManager.api.transaction.dto.TransactionResponse;
import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.transaction.Transaction;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionService;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

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
    private Jwt jwt;

    @InjectMocks
    private TransactionController transactionController;

    @Test
    void shouldCreateTransaction() {
        Long userId = 1L;
        Long accountId = 10L;
        Long categoryId = 20L;

        TransactionRequest request = new TransactionRequest(
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            accountId,
            categoryId
        );

        Transaction transaction = createMockTransaction(
            100L,
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            accountId,
            "Nubank",
            categoryId,
            "Alimentação"
        );

        when(jwt.getSubject())
            .thenReturn(userId.toString());

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
            transactionController.create(
                jwt,
                request
            );

        assertEquals(
            HttpStatus.CREATED,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            100L,
            response.getBody().id()
        );

        assertEquals(
            "Mercado",
            response.getBody().description()
        );

        assertEquals(
            new BigDecimal("150.00"),
            response.getBody().amount()
        );

        assertEquals(
            TransactionType.EXPENSE,
            response.getBody().type()
        );

        assertEquals(
            accountId,
            response.getBody().accountId()
        );

        assertEquals(
            "Nubank",
            response.getBody().accountName()
        );

        assertEquals(
            categoryId,
            response.getBody().categoryId()
        );

        assertEquals(
            "Alimentação",
            response.getBody().categoryName()
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
    void shouldListTransactions() {
        Long userId = 1L;

        Transaction firstTransaction = createMockTransaction(
            100L,
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            10L,
            "Nubank",
            20L,
            "Alimentação"
        );

        Transaction secondTransaction = createMockTransaction(
            101L,
            "Salário",
            new BigDecimal("3000.00"),
            LocalDate.of(2026, 10, 1),
            TransactionType.INCOME,
            10L,
            "Nubank",
            30L,
            "Salário"
        );

        when(jwt.getSubject())
            .thenReturn(userId.toString());

        when(transactionService.listByUser(userId))
            .thenReturn(List.of(
                firstTransaction,
                secondTransaction
            ));

        ResponseEntity<List<TransactionResponse>> response =
            transactionController.list(jwt);

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            2,
            response.getBody().size()
        );

        assertEquals(
            "Mercado",
            response.getBody().get(0).description()
        );

        assertEquals(
            "Salário",
            response.getBody().get(1).description()
        );

        verify(transactionService)
            .listByUser(userId);
    }

    @Test
    void shouldFindTransactionById() {
        Long userId = 1L;
        Long transactionId = 100L;

        Transaction transaction = createMockTransaction(
            transactionId,
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            10L,
            "Nubank",
            20L,
            "Alimentação"
        );

        when(jwt.getSubject())
            .thenReturn(userId.toString());

        when(transactionService.findById(
            userId,
            transactionId
        )).thenReturn(transaction);

        ResponseEntity<TransactionResponse> response =
            transactionController.findById(
                jwt,
                transactionId
            );

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            transactionId,
            response.getBody().id()
        );

        assertEquals(
            "Mercado",
            response.getBody().description()
        );

        verify(transactionService).findById(
            userId,
            transactionId
        );
    }

    @Test
    void shouldUpdateTransaction() {
        Long userId = 1L;
        Long transactionId = 100L;

        TransactionRequest request = new TransactionRequest(
            "Uber",
            new BigDecimal("35.90"),
            LocalDate.of(2026, 10, 2),
            TransactionType.EXPENSE,
            11L,
            21L
        );

        Transaction transaction = createMockTransaction(
            transactionId,
            "Uber",
            new BigDecimal("35.90"),
            LocalDate.of(2026, 10, 2),
            TransactionType.EXPENSE,
            11L,
            "Carteira",
            21L,
            "Transporte"
        );

        when(jwt.getSubject())
            .thenReturn(userId.toString());

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
                jwt,
                transactionId,
                request
            );

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            "Uber",
            response.getBody().description()
        );

        assertEquals(
            new BigDecimal("35.90"),
            response.getBody().amount()
        );

        assertEquals(
            "Carteira",
            response.getBody().accountName()
        );

        assertEquals(
            "Transporte",
            response.getBody().categoryName()
        );

        verify(transactionService).update(
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

        when(jwt.getSubject())
            .thenReturn(userId.toString());

        ResponseEntity<Void> response =
            transactionController.delete(
                jwt,
                transactionId
            );

        assertEquals(
            HttpStatus.NO_CONTENT,
            response.getStatusCode()
        );

        assertNull(response.getBody());

        verify(transactionService).delete(
            userId,
            transactionId
        );
    }

    private Transaction createMockTransaction(
        Long transactionId,
        String description,
        BigDecimal amount,
        LocalDate date,
        TransactionType type,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName
    ) {
        Account account = mock(Account.class);
        Category category = mock(Category.class);
        Transaction transaction = mock(Transaction.class);

        when(account.getId())
            .thenReturn(accountId);

        when(account.getName())
            .thenReturn(accountName);

        when(category.getId())
            .thenReturn(categoryId);

        when(category.getName())
            .thenReturn(categoryName);

        when(transaction.getId())
            .thenReturn(transactionId);

        when(transaction.getDescription())
            .thenReturn(description);

        when(transaction.getAmount())
            .thenReturn(amount);

        when(transaction.getDate())
            .thenReturn(date);

        when(transaction.getType())
            .thenReturn(type);

        when(transaction.getAccount())
            .thenReturn(account);

        when(transaction.getCategory())
            .thenReturn(category);

        return transaction;
    }
}
