package io.github.gabrielhdias.financeManager.api.transaction.dto;

import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(

    @NotBlank(message = "A descrição é obrigatória")
    @Size(
        max = 255,
        message = "A descrição deve possuir no máximo 255 caracteres"
    )
    String description,

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser maior que zero")
    @Digits(
        integer = 17,
        fraction = 2,
        message = "O valor deve possuir no máximo 17 dígitos inteiros e 2 casas decimais"
    )
    BigDecimal amount,

    @NotNull(message = "A data é obrigatória")
    LocalDate date,

    @NotNull(message = "O tipo da transação é obrigatório")
    TransactionType type,

    @NotNull(message = "A conta é obrigatória")
    Long accountId,

    @NotNull(message = "A categoria é obrigatória")
    Long categoryId
) {
}
