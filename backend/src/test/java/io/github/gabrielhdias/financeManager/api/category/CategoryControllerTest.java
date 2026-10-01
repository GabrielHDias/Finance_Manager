package io.github.gabrielhdias.financeManager.api.category;

import io.github.gabrielhdias.financeManager.api.category.dto.CategoryRequest;
import io.github.gabrielhdias.financeManager.api.category.dto.CategoryResponse;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private CategoryController categoryController;

    @Test
    void shouldCreateCategory() {
        Long userId = 1L;

        CategoryRequest request =
            new CategoryRequest("Alimentação");

        Category category = mock(Category.class);

        when(jwt.getSubject())
            .thenReturn(userId.toString());

        when(category.getId())
            .thenReturn(10L);

        when(category.getName())
            .thenReturn("Alimentação");

        when(categoryService.create(
            userId,
            "Alimentação"
        )).thenReturn(category);

        ResponseEntity<CategoryResponse> response =
            categoryController.create(
                jwt,
                request
            );

        assertEquals(
            HttpStatus.CREATED,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            10L,
            response.getBody().id()
        );

        assertEquals(
            "Alimentação",
            response.getBody().name()
        );

        verify(categoryService).create(
            userId,
            "Alimentação"
        );
    }

    @Test
    void shouldListCategories() {
        Long userId = 1L;

        Category firstCategory = mock(Category.class);
        Category secondCategory = mock(Category.class);

        when(jwt.getSubject())
            .thenReturn(userId.toString());

        when(firstCategory.getId())
            .thenReturn(10L);

        when(firstCategory.getName())
            .thenReturn("Alimentação");

        when(secondCategory.getId())
            .thenReturn(20L);

        when(secondCategory.getName())
            .thenReturn("Transporte");

        when(categoryService.listByUser(userId))
            .thenReturn(List.of(
                firstCategory,
                secondCategory
            ));

        ResponseEntity<List<CategoryResponse>> response =
            categoryController.list(jwt);

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            2,
            response.getBody().size()
        );

        assertEquals(
            "Alimentação",
            response.getBody().get(0).name()
        );

        assertEquals(
            "Transporte",
            response.getBody().get(1).name()
        );

        verify(categoryService)
            .listByUser(userId);
    }

    @Test
    void shouldFindCategoryById() {
        Long userId = 1L;
        Long categoryId = 10L;

        Category category = mock(Category.class);

        when(jwt.getSubject())
            .thenReturn(userId.toString());

        when(category.getId())
            .thenReturn(categoryId);

        when(category.getName())
            .thenReturn("Alimentação");

        when(categoryService.findById(
            userId,
            categoryId
        )).thenReturn(category);

        ResponseEntity<CategoryResponse> response =
            categoryController.findById(
                jwt,
                categoryId
            );

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            categoryId,
            response.getBody().id()
        );

        assertEquals(
            "Alimentação",
            response.getBody().name()
        );

        verify(categoryService).findById(
            userId,
            categoryId
        );
    }

    @Test
    void shouldRenameCategory() {
        Long userId = 1L;
        Long categoryId = 10L;

        CategoryRequest request =
            new CategoryRequest("Mercado");

        Category category = mock(Category.class);

        when(jwt.getSubject())
            .thenReturn(userId.toString());

        when(category.getId())
            .thenReturn(categoryId);

        when(category.getName())
            .thenReturn("Mercado");

        when(categoryService.rename(
            userId,
            categoryId,
            "Mercado"
        )).thenReturn(category);

        ResponseEntity<CategoryResponse> response =
            categoryController.rename(
                jwt,
                categoryId,
                request
            );

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            categoryId,
            response.getBody().id()
        );

        assertEquals(
            "Mercado",
            response.getBody().name()
        );

        verify(categoryService).rename(
            userId,
            categoryId,
            "Mercado"
        );
    }

    @Test
    void shouldDeleteCategory() {
        Long userId = 1L;
        Long categoryId = 10L;

        when(jwt.getSubject())
            .thenReturn(userId.toString());

        ResponseEntity<Void> response =
            categoryController.delete(
                jwt,
                categoryId
            );

        assertEquals(
            HttpStatus.NO_CONTENT,
            response.getStatusCode()
        );

        assertNull(response.getBody());

        verify(categoryService).delete(
            userId,
            categoryId
        );
    }
}
