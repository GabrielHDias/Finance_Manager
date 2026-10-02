package io.github.gabrielhdias.financeManager.api.category;

import io.github.gabrielhdias.financeManager.api.category.dto.CategoryRequest;
import io.github.gabrielhdias.financeManager.api.category.dto.CategoryResponse;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryService;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@Tag(
    name = "Categories",
    description = "Gerenciamento das categorias financeiras do usuário"
)
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthenticatedUser authenticatedUser;

    public CategoryController(
        CategoryService categoryService,
        AuthenticatedUser authenticatedUser
    ) {
        this.categoryService = categoryService;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
        @Valid @RequestBody CategoryRequest request
    ) {
        Long userId = authenticatedUser.getId();

        Category category = categoryService.create(
            userId,
            request.name()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(CategoryMapper.toResponse(category));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list() {
        Long userId = authenticatedUser.getId();

        List<CategoryResponse> response = categoryService
            .listByUser(userId)
            .stream()
            .map(CategoryMapper::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(
        @PathVariable Long id
    ) {
        Long userId = authenticatedUser.getId();

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
        @PathVariable Long id,
        @Valid @RequestBody CategoryRequest request
    ) {
        Long userId = authenticatedUser.getId();

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
        @PathVariable Long id
    ) {
        Long userId = authenticatedUser.getId();

        categoryService.delete(
            userId,
            id
        );

        return ResponseEntity.noContent().build();
    }
}
