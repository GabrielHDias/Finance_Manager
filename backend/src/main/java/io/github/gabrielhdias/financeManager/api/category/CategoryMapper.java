package io.github.gabrielhdias.financeManager.api.category;

import io.github.gabrielhdias.financeManager.api.category.dto.CategoryResponse;
import io.github.gabrielhdias.financeManager.domain.category.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName()
        );
    }
}
