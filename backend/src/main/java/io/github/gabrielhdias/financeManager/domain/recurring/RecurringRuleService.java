package io.github.gabrielhdias.financeManager.domain.recurring;

import io.github.gabrielhdias.financeManager.domain.account.AccountRepository;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryRepository;
import io.github.gabrielhdias.financeManager.domain.category.CategoryType;
import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringRuleService {
    private final RecurringRuleRepository rules;
    private final AccountRepository accounts;
    private final CategoryRepository categories;
    private final RecurringGenerationService generation;

    public RecurringRuleService(RecurringRuleRepository rules, AccountRepository accounts, CategoryRepository categories,
                                RecurringGenerationService generation) {
        this.rules = rules;
        this.accounts = accounts;
        this.categories = categories;
        this.generation = generation;
    }

    @Transactional
    public RecurringRule create(Long userId, String description, BigDecimal amount, TransactionType type,
                                RecurringFrequency frequency, LocalDate startDate, LocalDate endDate,
                                Long accountId, Long categoryId) {
        if (type != TransactionType.INCOME && type != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("A recorrência aceita apenas receita ou despesa");
        }
        if (description == null || description.isBlank() || description.trim().length() > 255) {
            throw new IllegalArgumentException("A descrição deve possuir de 1 a 255 caracteres");
        }
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new IllegalArgumentException("Informe um valor positivo com até duas casas decimais");
        }
        if (startDate == null || (endDate != null && endDate.isBefore(startDate))) {
            throw new IllegalArgumentException("Informe um período válido");
        }
        var account = accounts.findByIdAndUserId(accountId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        Category category = categories.findByIdAndUserId(categoryId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        if (!category.getType().name().equals(type.name())) {
            throw new BusinessRuleException("A categoria precisa ser do mesmo tipo da transação");
        }
        RecurringRule rule = rules.save(new RecurringRule(
            account, category, description, amount.setScale(2), type, frequency, startDate, endDate
        ));
        generation.generate(userId);
        return rule;
    }

    @Transactional(readOnly = true)
    public List<RecurringRule> list(Long userId) {
        return rules.findByAccountUserIdAndActiveTrue(userId);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        RecurringRule rule = rules.findByIdAndAccountUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recorrência não encontrada"));
        rule.deactivate();
    }
}
