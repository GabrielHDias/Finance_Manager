package io.github.gabrielhdias.financeManager.api.category;

import io.github.gabrielhdias.financeManager.api.category.dto.CategoryRequest;
import io.github.gabrielhdias.financeManager.api.category.dto.CategoryResponse;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CategoryRequest request
    ) {
        Long userId = getUserId(jwt);

        Category category = categoryService.create(
            userId,
            request.name()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(CategoryMapper.toResponse(category));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        List<CategoryResponse> response = categoryService
            .listByUser(userId)
            .stream()
            .map(CategoryMapper::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id
    ) {
        Long userId = getUserId(jwt);

        Category category = categoryService.findById(
            userId,
            id
        );

        return ResponseEntity.ok(
            CategoryMapper.toResponse(category)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> rename(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @Valid @RequestBody CategoryRequest request
    ) {
        Long userId = getUserId(jwt);

        Category category = categoryService.rename(
            userId,
            id,
            request.name()
        );

        return ResponseEntity.ok(
            CategoryMapper.toResponse(category)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id
    ) {
        Long userId = getUserId(jwt);

        categoryService.delete(
            userId,
            id
        );

        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
