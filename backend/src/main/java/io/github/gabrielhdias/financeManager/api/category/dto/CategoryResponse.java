package io.github.gabrielhdias.financeManager.api.category.dto;

import io.github.gabrielhdias.financeManager.domain.category.CategoryType;

public record CategoryResponse(
    Long id,
    String name,
    CategoryType type
) {
}
