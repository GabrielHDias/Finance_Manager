package io.github.gabrielhdias.financeManager.api.account;

import io.github.gabrielhdias.financeManager.api.account.dto.AccountRequest;
import io.github.gabrielhdias.financeManager.api.account.dto.AccountResponse;
import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.account.AccountService;
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
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AuthenticatedUser authenticatedUser;

    public AccountController(
        AccountService accountService,
        AuthenticatedUser authenticatedUser
    ) {
        this.accountService = accountService;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
        @Valid @RequestBody AccountRequest request
    ) {
        Long userId = authenticatedUser.getId();

        Account account = accountService.create(
            userId,
            request.name()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(AccountMapper.toResponse(account));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> list() {
        Long userId = authenticatedUser.getId();

        List<AccountResponse> response = accountService
            .listByUser(userId)
            .stream()
            .map(AccountMapper::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findById(
        @PathVariable Long id
    ) {
        Long userId = authenticatedUser.getId();

        Account account = accountService.findById(
            userId,
            id
        );

        return ResponseEntity.ok(
            AccountMapper.toResponse(account)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> rename(
        @PathVariable Long id,
        @Valid @RequestBody AccountRequest request
    ) {
        Long userId = authenticatedUser.getId();

        Account account = accountService.rename(
            userId,
            id,
            request.name()
        );

        return ResponseEntity.ok(
            AccountMapper.toResponse(account)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable Long id
    ) {
        Long userId = authenticatedUser.getId();

        accountService.delete(
            userId,
            id
        );

        return ResponseEntity.noContent().build();
    }
}
