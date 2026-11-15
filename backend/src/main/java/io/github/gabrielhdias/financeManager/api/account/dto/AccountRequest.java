package io.github.gabrielhdias.financeManager.api.account.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.annotation.JsonSerialize;
import io.github.gabrielhdias.financeManager.api.money.MoneySerializer;
import io.swagger.v3.oas.annotations.media.Schema;

public record AccountRequest(

    @NotBlank(message = "O nome da conta é obrigatório")
    @Size(
        max = 100,
        message = "O nome da conta deve possuir no máximo 100 caracteres"
    )
    String name,

    @Digits(
        integer = 17,
        fraction = 2,
        message = "O saldo inicial deve ter no máximo 17 dígitos inteiros e 2 casas decimais"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonSerialize(using = MoneySerializer.class)
    @Schema(type = "string", implementation = String.class, example = "0.00")
    BigDecimal openingBalance
) {
    public AccountRequest(String name) {
        this(name, null);
    }
}
