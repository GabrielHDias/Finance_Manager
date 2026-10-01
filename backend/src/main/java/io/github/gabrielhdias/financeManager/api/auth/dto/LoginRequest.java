package io.github.gabrielhdias.financeManager.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email deve possuir um formato válido")
    String email,

    @NotBlank(message = "A senha é obrigatória")
    String password
) {
}
