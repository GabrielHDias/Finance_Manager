package io.github.gabrielhdias.financeManager.api.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

    @NotBlank(message = "O nome da categoria é obrigatório")
    @Size(
        max = 100,
        message = "O nome da categoria deve possuir no máximo 100 caracteres"
    )
    String name
) {
}
