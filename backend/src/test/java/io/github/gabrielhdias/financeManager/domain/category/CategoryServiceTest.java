package io.github.gabrielhdias.financeManager.domain.category;

import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldCreateCategoryWhenNameIsAvailable() {
        Long userId = 1L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

        when(categoryRepository.existsByUserIdAndName(
            userId,
            "Alimentação"
        )).thenReturn(false);

        when(categoryRepository.save(any(Category.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.create(
            userId,
            "Alimentação"
        );

        assertNotNull(result);
        assertEquals("Alimentação", result.getName());
        assertEquals(user, result.getUser());

        verify(categoryRepository)
            .save(any(Category.class));
    }

    @Test
    void shouldNotCreateCategoryWhenNameAlreadyExists() {
        Long userId = 1L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

        when(categoryRepository.existsByUserIdAndName(
            userId,
            "Alimentação"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> categoryService.create(
                userId,
                "Alimentação"
            )
        );

        assertEquals(
            "Já existe uma categoria com esse nome para o usuário",
            exception.getMessage()
        );

        verify(categoryRepository, never())
            .save(any(Category.class));
    }

    @Test
    void shouldNotCreateCategoryWhenUserDoesNotExist() {
        Long userId = 1L;

        when(userRepository.findById(userId))
            .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> categoryService.create(
                userId,
                "Alimentação"
            )
        );

        assertEquals(
            "Usuário não encontrado",
            exception.getMessage()
        );

        verify(categoryRepository, never())
            .save(any(Category.class));
    }

    @Test
    void shouldListCategoriesByUser() {
        Long userId = 1L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        List<Category> categories = List.of(
            new Category("Alimentação", user),
            new Category("Transporte", user)
        );

        when(categoryRepository.findAllByUserId(userId))
            .thenReturn(categories);

        List<Category> result =
            categoryService.listByUser(userId);

        assertEquals(2, result.size());
        assertEquals(categories, result);

        verify(categoryRepository)
            .findAllByUserId(userId);
    }

    @Test
    void shouldFindCategoryByIdAndUser() {
        Long userId = 1L;
        Long categoryId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        Category category =
            new Category("Alimentação", user);

        when(categoryRepository.findByIdAndUserId(
            categoryId,
            userId
        )).thenReturn(Optional.of(category));

        Category result = categoryService.findById(
            userId,
            categoryId
        );

        assertEquals(category, result);

        verify(categoryRepository)
            .findByIdAndUserId(categoryId, userId);
    }

    @Test
    void shouldNotFindCategoryWhenCategoryDoesNotExistForUser() {
        Long userId = 1L;
        Long categoryId = 10L;

        when(categoryRepository.findByIdAndUserId(
            categoryId,
            userId
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> categoryService.findById(
                userId,
                categoryId
            )
        );

        assertEquals(
            "Categoria não encontrada",
            exception.getMessage()
        );
    }

    @Test
    void shouldRenameCategory() {
        Long userId = 1L;
        Long categoryId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        Category category =
            new Category("Alimentação", user);

        when(categoryRepository.findByIdAndUserId(
            categoryId,
            userId
        )).thenReturn(Optional.of(category));

        when(categoryRepository.existsByUserIdAndName(
            userId,
            "Mercado"
        )).thenReturn(false);

        Category result = categoryService.rename(
            userId,
            categoryId,
            "Mercado"
        );

        assertEquals(
            "Mercado",
            result.getName()
        );
    }

    @Test
    void shouldNotRenameCategoryWhenNameAlreadyExists() {
        Long userId = 1L;
        Long categoryId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        Category category =
            new Category("Alimentação", user);

        when(categoryRepository.findByIdAndUserId(
            categoryId,
            userId
        )).thenReturn(Optional.of(category));

        when(categoryRepository.existsByUserIdAndName(
            userId,
            "Transporte"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> categoryService.rename(
                userId,
                categoryId,
                "Transporte"
            )
        );

        assertEquals(
            "Já existe uma categoria com esse nome para o usuário",
            exception.getMessage()
        );

        assertEquals(
            "Alimentação",
            category.getName()
        );
    }

    @Test
    void shouldDeleteCategoryWithoutTransactions() {
        Long userId = 1L;
        Long categoryId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        Category category =
            new Category("Alimentação", user);

        when(categoryRepository.findByIdAndUserId(
            categoryId,
            userId
        )).thenReturn(Optional.of(category));

        when(transactionRepository.existsByCategoryId(categoryId))
            .thenReturn(false);

        categoryService.delete(
            userId,
            categoryId
        );

        verify(categoryRepository)
            .delete(category);
    }

    @Test
    void shouldNotDeleteCategoryWithTransactions() {
        Long userId = 1L;
        Long categoryId = 10L;

        User user = new User(
            "Gabriel",
            "gabriel@email.com",
            "password"
        );

        Category category =
            new Category("Alimentação", user);

        when(categoryRepository.findByIdAndUserId(
            categoryId,
            userId
        )).thenReturn(Optional.of(category));

        when(transactionRepository.existsByCategoryId(categoryId))
            .thenReturn(true);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> categoryService.delete(
                userId,
                categoryId
            )
        );

        assertEquals(
            "Não é possível excluir uma categoria que possui transações",
            exception.getMessage()
        );

        verify(categoryRepository, never())
            .delete(any(Category.class));
    }
}
