package io.github.gabrielhdias.financeManager.api.transaction;

import io.github.gabrielhdias.financeManager.api.transaction.dto.TransactionRequest;
import io.github.gabrielhdias.financeManager.api.transaction.dto.TransactionResponse;
import io.github.gabrielhdias.financeManager.domain.transaction.Transaction;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionService;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthenticatedUser authenticatedUser;

    public TransactionController(
        TransactionService transactionService,
        AuthenticatedUser authenticatedUser
    ) {
        this.transactionService = transactionService;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
        @Valid @RequestBody TransactionRequest request
    ) {
        Long userId = authenticatedUser.getId();

        Transaction transaction = transactionService.create(
            userId,
            request.description(),
            request.amount(),
            request.date(),
            request.type(),
            request.accountId(),
            request.categoryId()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(TransactionMapper.toResponse(transaction));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> list() {
        Long userId = authenticatedUser.getId();

        List<TransactionResponse> response = transactionService
            .listByUser(userId)
            .stream()
            .map(TransactionMapper::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> findById(
        @PathVariable Long id
    ) {
        Long userId = authenticatedUser.getId();

        Transaction transaction = transactionService.findById(
            userId,
            id
        );

        return ResponseEntity.ok(
            TransactionMapper.toResponse(transaction)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody TransactionRequest request
    ) {
        Long userId = authenticatedUser.getId();

        Transaction transaction = transactionService.update(
            userId,
            id,
            request.description(),
            request.amount(),
            request.date(),
            request.type(),
            request.accountId(),
            request.categoryId()
        );

        return ResponseEntity.ok(
            TransactionMapper.toResponse(transaction)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable Long id
    ) {
        Long userId = authenticatedUser.getId();

        transactionService.delete(
            userId,
            id
        );

        return ResponseEntity.noContent().build();
    }
}
