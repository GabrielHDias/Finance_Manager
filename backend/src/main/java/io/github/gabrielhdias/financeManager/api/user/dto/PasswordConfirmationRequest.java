package io.github.gabrielhdias.financeManager.api.user.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordConfirmationRequest(
    @NotBlank(message = "Informe a senha atual")
    String currentPassword
) {}
