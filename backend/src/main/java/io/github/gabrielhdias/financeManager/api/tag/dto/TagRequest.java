package io.github.gabrielhdias.financeManager.api.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(
    @NotBlank(message = "O nome da etiqueta é obrigatório")
    @Size(max = 40, message = "O nome da etiqueta deve possuir no máximo 40 caracteres")
    String name
) {
}
