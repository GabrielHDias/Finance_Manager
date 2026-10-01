package io.github.gabrielhdias.financeManager.api.auth.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {
}
