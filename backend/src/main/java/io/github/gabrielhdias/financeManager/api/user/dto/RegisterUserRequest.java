package io.github.gabrielhdias.financeManager.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(

    @NotBlank(message = "O nome é obrigatório")
    @Size(
        max = 100,
        message = "O nome deve possuir no máximo 100 caracteres"
    )
    String name,

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email deve possuir um formato válido")
    @Size(
        max = 255,
        message = "O email deve possuir no máximo 255 caracteres"
    )
    String email,

    @NotBlank(message = "A senha é obrigatória")
    @Size(
        min = 8,
        max = 100,
        message = "A senha deve possuir entre 8 e 100 caracteres"
    )
    String password
) {
}
