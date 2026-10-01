package io.github.gabrielhdias.financeManager.api.transaction;

import io.github.gabrielhdias.financeManager.api.transaction.dto.TransactionRequest;
import io.github.gabrielhdias.financeManager.api.transaction.dto.TransactionResponse;
import io.github.gabrielhdias.financeManager.domain.transaction.Transaction;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    public TransactionController(
        TransactionService transactionService
    ) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody TransactionRequest request
    ) {
        Long userId = getUserId(jwt);

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
    public ResponseEntity<List<TransactionResponse>> list(
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        List<TransactionResponse> response = transactionService
            .listByUser(userId)
            .stream()
            .map(TransactionMapper::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> findById(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id
    ) {
        Long userId = getUserId(jwt);

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
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @Valid @RequestBody TransactionRequest request
    ) {
        Long userId = getUserId(jwt);

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
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id
    ) {
        Long userId = getUserId(jwt);

        transactionService.delete(
            userId,
            id
        );

        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
