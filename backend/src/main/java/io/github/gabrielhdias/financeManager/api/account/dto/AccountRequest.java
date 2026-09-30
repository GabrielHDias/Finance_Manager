package io.github.gabrielhdias.financeManager.api.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountRequest(

    @NotBlank(message = "O nome da conta é obrigatório")
    @Size(
        max = 100,
        message = "O nome da conta deve possuir no máximo 100 caracteres"
    )
    String name
) {
}
