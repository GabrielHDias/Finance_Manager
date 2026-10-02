package io.github.gabrielhdias.financeManager.api.transaction;

import io.github.gabrielhdias.financeManager.api.transaction.dto.TransactionRequest;
import io.github.gabrielhdias.financeManager.api.transaction.dto.TransactionResponse;
import io.github.gabrielhdias.financeManager.domain.transaction.Transaction;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionService;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@Tag(
    name = "Transactions",
    description = "Gerenciamento das transações financeiras do usuário"
)
@SecurityRequirement(name = "bearerAuth")
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
    public ResponseEntity<List<TransactionResponse>> list(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        @RequestParam(required = false)
        TransactionType type,

        @RequestParam(required = false)
        Long accountId,

        @RequestParam(required = false)
        Long categoryId
    ) {
        Long userId = authenticatedUser.getId();

        List<TransactionResponse> response =
            transactionService
                .filterByUser(
                    userId,
                    startDate,
                    endDate,
                    type,
                    accountId,
                    categoryId
                )
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
