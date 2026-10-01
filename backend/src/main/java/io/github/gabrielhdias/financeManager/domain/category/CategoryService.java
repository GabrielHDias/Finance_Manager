package io.github.gabrielhdias.financeManager.domain.category;

import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserRepository;
import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.exception.DuplicateResourceException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(
        CategoryRepository categoryRepository,
        UserRepository userRepository,
        TransactionRepository transactionRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Category create(Long userId, String name) {
        User user = findUserById(userId);

        String normalizedName = name.trim();

        if (categoryRepository.existsByUserIdAndName(userId, normalizedName)) {
            throw new DuplicateResourceException(
                "Já existe uma categoria com esse nome para o usuário"
            );
        }

        Category category = new Category(name, user);

        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> listByUser(Long userId) {
        return categoryRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Category findById(Long userId, Long categoryId) {
        return findCategoryByIdAndUserId(categoryId, userId);
    }

    @Transactional
    public Category rename(Long userId, Long categoryId, String newName) {
        Category category = findCategoryByIdAndUserId(
            categoryId,
            userId
        );

        String normalizedName = newName.trim();

        if (!category.getName().equals(normalizedName)
            && categoryRepository.existsByUserIdAndName(
            userId,
            normalizedName
        )) {
            throw new DuplicateResourceException(
                "Já existe uma categoria com esse nome para o usuário"
            );
        }

        category.rename(newName);

        return category;
    }

    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category category = findCategoryByIdAndUserId(
            categoryId,
            userId
        );

        if (transactionRepository.existsByCategoryId(categoryId)) {
            throw new BusinessRuleException(
                "Não é possível excluir uma categoria que possui transações"
            );
        }

        categoryRepository.delete(category);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuário não encontrado"
            ));
    }

    private Category findCategoryByIdAndUserId(
        Long categoryId,
        Long userId
    ) {
        return categoryRepository.findByIdAndUserId(
                categoryId,
                userId
            )
            .orElseThrow(() -> new ResourceNotFoundException(
                "Categoria não encontrada"
            ));
    }
}
