package io.github.gabrielhdias.financeManager.domain.transaction;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.dashboard.CategoryExpenseSummary;
import io.github.gabrielhdias.financeManager.domain.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldCalculateBalanceFromIncomeAndExpenses() {
        TestData data = createTestData(
            "balance-test@email.com"
        );

        persistTransaction(
            "Salário",
            "1000.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.INCOME,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Mercado",
            "150.00",
            LocalDate.of(2026, 10, 2),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Uber",
            "35.90",
            LocalDate.of(2026, 10, 3),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        entityManager.flush();

        BigDecimal balance =
            transactionRepository.calculateBalanceByAccountId(
                data.account().getId(),
                TransactionType.INCOME
            );

        assertEquals(
            new BigDecimal("814.10"),
            balance
        );
    }

    @Test
    void shouldReturnZeroWhenAccountHasNoTransactions() {
        TestData data = createTestData(
            "empty-test@email.com"
        );

        entityManager.flush();

        BigDecimal balance =
            transactionRepository.calculateBalanceByAccountId(
                data.account().getId(),
                TransactionType.INCOME
            );

        assertEquals(
            0,
            balance.compareTo(BigDecimal.ZERO)
        );
    }

    @Test
    void shouldFilterTransactionsByType() {
        TestData data = createTestData(
            "type-filter@email.com"
        );

        persistTransaction(
            "Salário",
            "1000.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.INCOME,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Mercado",
            "150.00",
            LocalDate.of(2026, 10, 2),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        entityManager.flush();

        List<Transaction> result =
            transactionRepository.findAllByFilters(
                data.user().getId(),
                null,
                null,
                TransactionType.EXPENSE,
                null,
                null
            );

        assertEquals(1, result.size());

        assertEquals(
            TransactionType.EXPENSE,
            result.get(0).getType()
        );
    }

    @Test
    void shouldFilterTransactionsByPeriod() {
        TestData data = createTestData(
            "period-filter@email.com"
        );

        persistTransaction(
            "Antes",
            "10.00",
            LocalDate.of(2026, 9, 30),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Dentro",
            "20.00",
            LocalDate.of(2026, 10, 15),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Depois",
            "30.00",
            LocalDate.of(2026, 11, 1),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        entityManager.flush();

        List<Transaction> result =
            transactionRepository.findAllByFilters(
                data.user().getId(),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                null,
                null,
                null
            );

        assertEquals(1, result.size());

        assertEquals(
            "Dentro",
            result.get(0).getDescription()
        );
    }

    @Test
    void shouldFilterTransactionsByAccount() {
        User user = createUser(
            "account-filter@email.com"
        );

        Account firstAccount =
            new Account("Nubank", user);

        Account secondAccount =
            new Account("Carteira", user);

        Category category =
            new Category("Geral", user);

        entityManager.persist(firstAccount);
        entityManager.persist(secondAccount);
        entityManager.persist(category);

        persistTransaction(
            "Nubank transaction",
            "100.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            firstAccount,
            category
        );

        persistTransaction(
            "Carteira transaction",
            "50.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            secondAccount,
            category
        );

        entityManager.flush();

        List<Transaction> result =
            transactionRepository.findAllByFilters(
                user.getId(),
                null,
                null,
                null,
                firstAccount.getId(),
                null
            );

        assertEquals(1, result.size());

        assertEquals(
            "Nubank transaction",
            result.get(0).getDescription()
        );
    }

    @Test
    void shouldFilterTransactionsByCategory() {
        User user = createUser(
            "category-filter@email.com"
        );

        Account account =
            new Account("Nubank", user);

        Category food =
            new Category("Alimentação", user);

        Category transport =
            new Category("Transporte", user);

        entityManager.persist(account);
        entityManager.persist(food);
        entityManager.persist(transport);

        persistTransaction(
            "Mercado",
            "100.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            account,
            food
        );

        persistTransaction(
            "Uber",
            "50.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            account,
            transport
        );

        entityManager.flush();

        List<Transaction> result =
            transactionRepository.findAllByFilters(
                user.getId(),
                null,
                null,
                null,
                null,
                transport.getId()
            );

        assertEquals(1, result.size());

        assertEquals(
            "Uber",
            result.get(0).getDescription()
        );
    }

    @Test
    void shouldOnlyReturnTransactionsFromRequestedUser() {
        TestData firstUser =
            createTestData(
                "user-one@email.com"
            );

        TestData secondUser =
            createTestData(
                "user-two@email.com"
            );

        persistTransaction(
            "Usuário 1",
            "100.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            firstUser.account(),
            firstUser.category()
        );

        persistTransaction(
            "Usuário 2",
            "200.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            secondUser.account(),
            secondUser.category()
        );

        entityManager.flush();

        List<Transaction> result =
            transactionRepository.findAllByFilters(
                firstUser.user().getId(),
                null,
                null,
                null,
                null,
                null
            );

        assertEquals(1, result.size());

        assertEquals(
            "Usuário 1",
            result.get(0).getDescription()
        );
    }

    @Test
    void shouldSumIncomeByUserAndPeriod() {
        TestData data = createTestData(
            "summary-income@email.com"
        );

        persistTransaction(
            "Salário",
            "5000.00",
            LocalDate.of(2026, 10, 5),
            TransactionType.INCOME,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Freelance",
            "1000.00",
            LocalDate.of(2026, 10, 20),
            TransactionType.INCOME,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Receita fora do período",
            "2000.00",
            LocalDate.of(2026, 9, 1),
            TransactionType.INCOME,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Despesa",
            "500.00",
            LocalDate.of(2026, 10, 10),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        entityManager.flush();

        BigDecimal result =
            transactionRepository
                .sumAmountByUserAndTypeAndPeriod(
                    data.user().getId(),
                    TransactionType.INCOME,
                    LocalDate.of(2026, 10, 1),
                    LocalDate.of(2026, 10, 31)
                );

        assertEquals(
            new BigDecimal("6000.00"),
            result
        );
    }

    @Test
    void shouldSumExpensesByUserWithoutPeriod() {
        TestData data = createTestData(
            "summary-expense@email.com"
        );

        persistTransaction(
            "Mercado",
            "150.00",
            LocalDate.of(2026, 9, 1),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Uber",
            "35.90",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Salário",
            "5000.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.INCOME,
            data.account(),
            data.category()
        );

        entityManager.flush();

        BigDecimal result =
            transactionRepository
                .sumAmountByUserAndTypeAndPeriod(
                    data.user().getId(),
                    TransactionType.EXPENSE,
                    null,
                    null
                );

        assertEquals(
            new BigDecimal("185.90"),
            result
        );
    }

    @Test
    void shouldReturnZeroWhenUserHasNoTransactionsOfRequestedType() {
        TestData data = createTestData(
            "summary-zero@email.com"
        );

        persistTransaction(
            "Mercado",
            "150.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        entityManager.flush();

        BigDecimal result =
            transactionRepository
                .sumAmountByUserAndTypeAndPeriod(
                    data.user().getId(),
                    TransactionType.INCOME,
                    null,
                    null
                );

        assertEquals(
            0,
            result.compareTo(BigDecimal.ZERO)
        );
    }

    @Test
    void shouldGroupExpensesByCategoryAndOrderByHighestAmount() {
        User user = createUser(
            "expense-category@email.com"
        );

        Account account =
            new Account("Nubank", user);

        Category food =
            new Category("Alimentação", user);

        Category transport =
            new Category("Transporte", user);

        entityManager.persist(account);
        entityManager.persist(food);
        entityManager.persist(transport);

        persistTransaction(
            "Mercado",
            "500.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            account,
            food
        );

        persistTransaction(
            "Restaurante",
            "350.00",
            LocalDate.of(2026, 10, 5),
            TransactionType.EXPENSE,
            account,
            food
        );

        persistTransaction(
            "Uber",
            "320.50",
            LocalDate.of(2026, 10, 10),
            TransactionType.EXPENSE,
            account,
            transport
        );

        entityManager.flush();

        List<CategoryExpenseSummary> result =
            transactionRepository.sumExpensesByCategory(
                user.getId(),
                TransactionType.EXPENSE,
                null,
                null
            );

        assertEquals(2, result.size());

        assertEquals(
            "Alimentação",
            result.get(0).categoryName()
        );

        assertEquals(
            new BigDecimal("850.00"),
            result.get(0).amount()
        );

        assertEquals(
            "Transporte",
            result.get(1).categoryName()
        );

        assertEquals(
            new BigDecimal("320.50"),
            result.get(1).amount()
        );
    }

    @Test
    void shouldIgnoreIncomeWhenGroupingExpensesByCategory() {
        TestData data = createTestData(
            "expense-only@email.com"
        );

        persistTransaction(
            "Mercado",
            "200.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Salário",
            "5000.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.INCOME,
            data.account(),
            data.category()
        );

        entityManager.flush();

        List<CategoryExpenseSummary> result =
            transactionRepository.sumExpensesByCategory(
                data.user().getId(),
                TransactionType.EXPENSE,
                null,
                null
            );

        assertEquals(1, result.size());

        assertEquals(
            new BigDecimal("200.00"),
            result.get(0).amount()
        );
    }

    @Test
    void shouldGroupExpensesByCategoryWithinPeriod() {
        TestData data = createTestData(
            "expense-period@email.com"
        );

        persistTransaction(
            "Antes",
            "100.00",
            LocalDate.of(2026, 9, 30),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Dentro 1",
            "200.00",
            LocalDate.of(2026, 10, 10),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Dentro 2",
            "50.00",
            LocalDate.of(2026, 10, 20),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        persistTransaction(
            "Depois",
            "300.00",
            LocalDate.of(2026, 11, 1),
            TransactionType.EXPENSE,
            data.account(),
            data.category()
        );

        entityManager.flush();

        List<CategoryExpenseSummary> result =
            transactionRepository.sumExpensesByCategory(
                data.user().getId(),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)
            );

        assertEquals(1, result.size());

        assertEquals(
            new BigDecimal("250.00"),
            result.get(0).amount()
        );
    }

    @Test
    void shouldOnlyGroupExpensesFromRequestedUser() {
        TestData firstUser =
            createTestData(
                "expense-user-one@email.com"
            );

        TestData secondUser =
            createTestData(
                "expense-user-two@email.com"
            );

        persistTransaction(
            "Usuário 1",
            "100.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            firstUser.account(),
            firstUser.category()
        );

        persistTransaction(
            "Usuário 2",
            "900.00",
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            secondUser.account(),
            secondUser.category()
        );

        entityManager.flush();

        List<CategoryExpenseSummary> result =
            transactionRepository.sumExpensesByCategory(
                firstUser.user().getId(),
                TransactionType.EXPENSE,
                null,
                null
            );

        assertEquals(1, result.size());

        assertEquals(
            new BigDecimal("100.00"),
            result.get(0).amount()
        );
    }

    private TestData createTestData(String email) {
        User user = createUser(email);

        Account account =
            new Account("Conta Teste", user);

        Category category =
            new Category("Categoria Teste", user);

        entityManager.persist(account);
        entityManager.persist(category);

        return new TestData(
            user,
            account,
            category
        );
    }

    private User createUser(String email) {
        User user = new User(
            "Gabriel",
            email,
            "$2a$10$encodedPassword"
        );

        entityManager.persist(user);

        return user;
    }

    private void persistTransaction(
        String description,
        String amount,
        LocalDate date,
        TransactionType type,
        Account account,
        Category category
    ) {
        Transaction transaction =
            new Transaction(
                description,
                new BigDecimal(amount),
                date,
                type,
                account,
                category
            );

        entityManager.persist(transaction);
    }

    private record TestData(
        User user,
        Account account,
        Category category
    ) {
    }
}
