package io.github.gabrielhdias.financeManager.api.account;

import io.github.gabrielhdias.financeManager.api.account.dto.AccountResponse;
import io.github.gabrielhdias.financeManager.domain.account.Account;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getName()
        );
    }
}
