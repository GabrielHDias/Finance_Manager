package io.github.gabrielhdias.financeManager.domain.budget;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryService;
import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionService;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.integration.AbstractPostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BudgetIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    @Autowired BudgetService budgets;
    @Autowired CategoryService categories;
    @Autowired TransactionService transactions;
    @Autowired EntityManager em;

    private record Fixture(User user, Account account, Category category) {}
    private Fixture fixture(String email) {
        User user = new User("Teste orçamento", email, "encoded-password");
        em.persist(user);
        Account account = new Account("Conta", user);
        Category category = new Category("Categoria", user);
        em.persist(account); em.persist(category); em.flush();
        return new Fixture(user, account, category);
    }
    private void transaction(Fixture f, LocalDate date, TransactionType type, String amount) {
        transactions.create(f.user().getId(), "Teste", new BigDecimal(amount), date, type, f.account().getId(), f.category().getId());
    }

    @Test
    void shouldCountOnlyOwnedMonthlyExpensesAndUpdateWithoutDuplicates() {
        Fixture owner = fixture("budget-owner@example.test");
        Fixture foreign = fixture("budget-foreign@example.test");
        transaction(owner, LocalDate.of(2026, 10, 1), TransactionType.EXPENSE, "60.00");
        transaction(owner, LocalDate.of(2026, 10, 31), TransactionType.INCOME, "999.00");
        transaction(owner, LocalDate.of(2026, 9, 30), TransactionType.EXPENSE, "90.00");
        transaction(foreign, LocalDate.of(2026, 10, 1), TransactionType.EXPENSE, "900.00");
        BudgetSummary first = budgets.save(owner.user().getId(), owner.category().getId(), 2026, 10, new BigDecimal("100.00"));
        assertEquals(new BigDecimal("60.00"), first.spent());
        assertEquals(new BigDecimal("40.00"), first.remaining());
        BudgetSummary updated = budgets.save(owner.user().getId(), owner.category().getId(), 2026, 10, new BigDecimal("50.00"));
        assertEquals(first.id(), updated.id());
        assertEquals(new BigDecimal("-10.00"), updated.remaining());
        assertEquals(1, budgets.list(owner.user().getId(), 2026, 10).size());
        assertTrue(budgets.list(foreign.user().getId(), 2026, 10).isEmpty());
        assertTrue(budgets.list(owner.user().getId(), 2026, 11).isEmpty());
    }

    @Test
    void shouldKeepMonthlyLimitsIndependentAndIncludeAllOwnedAccounts() {
        Fixture owner = fixture("budget-months@example.test");
        Account second = new Account("Segunda", owner.user()); em.persist(second);
        transaction(owner, LocalDate.of(2026, 10, 2), TransactionType.EXPENSE, "20.00");
        transactions.create(owner.user().getId(), "Outra conta", new BigDecimal("30.00"), LocalDate.of(2026, 10, 3), TransactionType.EXPENSE, second.getId(), owner.category().getId());
        budgets.save(owner.user().getId(), owner.category().getId(), 2026, 10, new BigDecimal("100.00"));
        budgets.save(owner.user().getId(), owner.category().getId(), 2026, 11, new BigDecimal("200.00"));
        assertEquals(new BigDecimal("50.00"), budgets.list(owner.user().getId(), 2026, 10).getFirst().spent());
        assertEquals(new BigDecimal("200.00"), budgets.list(owner.user().getId(), 2026, 11).getFirst().remaining());
    }

    @Test
    void shouldRejectForeignCategoriesAndBudgetDeletion() {
        Fixture owner = fixture("budget-isolated-owner@example.test");
        Fixture foreign = fixture("budget-isolated-foreign@example.test");
        BudgetSummary budget = budgets.save(owner.user().getId(), owner.category().getId(), 2026, 10, BigDecimal.TEN);
        assertThrows(ResourceNotFoundException.class, () -> budgets.save(foreign.user().getId(), owner.category().getId(), 2026, 10, BigDecimal.ONE));
        assertThrows(ResourceNotFoundException.class, () -> budgets.delete(foreign.user().getId(), budget.id()));
    }

    @Test
    void shouldProtectCategoryUntilItsBudgetsAreRemoved() {
        Fixture owner = fixture("budget-category@example.test");
        BudgetSummary budget = budgets.save(owner.user().getId(), owner.category().getId(), 2026, 10, BigDecimal.TEN);
        assertThrows(BusinessRuleException.class, () -> categories.delete(owner.user().getId(), owner.category().getId()));
        budgets.delete(owner.user().getId(), budget.id());
        em.flush();
        categories.delete(owner.user().getId(), owner.category().getId());
        em.flush();
    }

    @Test
    void shouldDeleteBudgetWithoutDeletingTransactionsAndReflectEditedExpenses() {
        Fixture owner = fixture("budget-delete@example.test");
        var transaction = transactions.create(owner.user().getId(), "Despesa", BigDecimal.TEN, LocalDate.of(2026, 10, 1), TransactionType.EXPENSE, owner.account().getId(), owner.category().getId());
        Long id = transaction.getId();
        var budget = budgets.save(owner.user().getId(), owner.category().getId(), 2026, 10, BigDecimal.TEN);
        assertEquals(0, budget.remaining().signum());
        transactions.update(owner.user().getId(), id, "Editada", new BigDecimal("5.00"), LocalDate.of(2026, 10, 1), TransactionType.EXPENSE, owner.account().getId(), owner.category().getId());
        assertEquals(new BigDecimal("5.00"), budgets.list(owner.user().getId(), 2026, 10).getFirst().remaining());
        budgets.delete(owner.user().getId(), budget.id());
        assertTrue(budgets.list(owner.user().getId(), 2026, 10).isEmpty());
        assertEquals(id, transactions.findById(owner.user().getId(), id).getId());
    }

    @Test
    void shouldValidatePeriodAndLimit() {
        assertThrows(IllegalArgumentException.class, () -> budgets.list(1L, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> budgets.list(1L, 2026, 13));
        assertThrows(IllegalArgumentException.class, () -> budgets.list(1L, 10000, 1));
        for (String amount : new String[] {"0", "-1", "1.001", "100000000000000000"}) {
            assertThrows(IllegalArgumentException.class, () -> budgets.save(1L, 1L, 2026, 10, new BigDecimal(amount)));
        }
    }

    @Test
    void shouldIncludeLeapDayWithoutIncludingMarch() {
        Fixture owner = fixture("budget-leap@example.test");
        transaction(owner, LocalDate.of(2024, 2, 29), TransactionType.EXPENSE, "29.00");
        transaction(owner, LocalDate.of(2024, 3, 1), TransactionType.EXPENSE, "100.00");
        assertEquals(new BigDecimal("29.00"), budgets.save(owner.user().getId(), owner.category().getId(), 2024, 2, new BigDecimal("100.00")).spent());
    }
}
