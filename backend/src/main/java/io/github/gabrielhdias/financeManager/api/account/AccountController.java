package io.github.gabrielhdias.financeManager.api.account;

import io.github.gabrielhdias.financeManager.api.account.dto.AccountRequest;
import io.github.gabrielhdias.financeManager.api.account.dto.AccountResponse;
import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.account.AccountService;
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
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody AccountRequest request
    ) {
        Long userId = getUserId(jwt);

        Account account = accountService.create(
            userId,
            request.name()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(AccountMapper.toResponse(account));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> list(
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        List<AccountResponse> response = accountService
            .listByUser(userId)
            .stream()
            .map(AccountMapper::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findById(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id
    ) {
        Long userId = getUserId(jwt);

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
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @Valid @RequestBody AccountRequest request
    ) {
        Long userId = getUserId(jwt);

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
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id
    ) {
        Long userId = getUserId(jwt);

        accountService.delete(
            userId,
            id
        );

        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
