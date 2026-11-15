package io.github.gabrielhdias.financeManager.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailChangeRequest(
    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email deve possuir um formato válido")
    @Size(max = 255, message = "O email deve possuir no máximo 255 caracteres")
    String email,
    @NotBlank(message = "Informe a senha atual")
    String currentPassword
) {}
