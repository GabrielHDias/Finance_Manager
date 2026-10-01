package io.github.gabrielhdias.financeManager.api.account.dto;

import java.math.BigDecimal;

public record AccountBalanceResponse(
    Long accountId,
    BigDecimal balance
) {
}
