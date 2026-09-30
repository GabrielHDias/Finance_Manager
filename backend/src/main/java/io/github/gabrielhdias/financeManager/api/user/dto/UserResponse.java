package io.github.gabrielhdias.financeManager.api.user.dto;

public record UserResponse(
    Long id,
    String name,
    String email
) {
}
