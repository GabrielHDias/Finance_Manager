package io.github.gabrielhdias.financeManager.domain.transaction;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.dashboard.CategoryExpenseSummary;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.integration.AbstractPostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class TransactionRepositoryTest
    extends AbstractPostgreSqlIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManager entityManager;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(TransactionType.class)
    void chartAggregationsRespectOwnerTypeInclusiveDatesAndPrecision(TransactionType type) {
        TestData owner = createTestData("chart-owner@example.test");
        TestData other = createTestData("chart-other@example.test");
        LocalDate start = LocalDate.of(2025, 12, 31);
        LocalDate end = LocalDate.of(2026, 1, 2);
        persistTransaction("Boundary start", "0.10", start, type, owner.account(), owner.category());
        persistTransaction("Same day", "0.20", start, type, owner.account(), owner.category());
        persistTransaction("Boundary end", "99999999999999999.99", end, type, owner.account(), owner.category());
        persistTransaction("Before", "100.00", start.minusDays(1), type, owner.account(), owner.category());
        persistTransaction("After", "100.00", end.plusDays(1), type, owner.account(), owner.category());
        persistTransaction("Other owner", "900.00", start, type, other.account(), other.category());
        persistTransaction("Other type", "900.00", start, type == TransactionType.INCOME ? TransactionType.EXPENSE : TransactionType.INCOME, owner.account(), owner.category());
        entityManager.flush();
        var service = new io.github.gabrielhdias.financeManager.domain.dashboard.DashboardService(transactionRepository);
        for (var group : io.github.gabrielhdias.financeManager.domain.dashboard.ChartGrouping.values()) {
            var summary = service.getTransactionsSummary(owner.user().getId(), type, group, start, end);
            assertEquals(new BigDecimal("100000000000000000.29"), summary.total());
        }
        var daily = transactionRepository.sumByDay(owner.user().getId(), type, start, end);
        assertEquals(2, daily.size());
        assertEquals(start, daily.getFirst().date());
        assertEquals(new BigDecimal("0.30"), daily.getFirst().amount());
        var monthly = transactionRepository.sumByMonth(owner.user().getId(), type, start, end);
        assertEquals(2025, monthly.getFirst().year());
        assertEquals(12, monthly.getFirst().month());
        assertEquals(2026, monthly.getLast().year());
        assertEquals(1, monthly.getLast().month());
    }

    @Test
    void chartMonthlyDoesNotCombineJanuaryOfDifferentYears() {
        TestData owner = createTestData("chart-years@example.test");
        persistTransaction("First January", "10.10", LocalDate.of(2025, 1, 31), TransactionType.INCOME, owner.account(), owner.category());
        persistTransaction("Next January", "20.20", LocalDate.of(2026, 1, 1), TransactionType.INCOME, owner.account(), owner.category());
        entityManager.flush();
        var rows = transactionRepository.sumByMonth(owner.user().getId(), TransactionType.INCOME, LocalDate.of(2025, 1, 31), LocalDate.of(2026, 1, 1));
        assertEquals(2, rows.size());
        assertEquals(new BigDecimal("10.10"), rows.getFirst().amount());
        assertEquals(new BigDecimal("20.20"), rows.getLast().amount());
    }

    @Test
    void batchedBalancesIncludeEmptyAccountsAndIsolateOwnersAtMaximumPrecision() {
        TestData owner = createTestData("batch-owner@example.test");
        TestData other = createTestData("batch-other@example.test");
        Account empty = new Account("Empty", owner.account().getUser()); entityManager.persist(empty);
        persistTransaction("High A", "99999999999999999.99", LocalDate.of(2026,10,1), TransactionType.INCOME, owner.account(), owner.category());
        persistTransaction("High B", "99999999999999999.99", LocalDate.of(2026,10,1), TransactionType.INCOME, owner.account(), owner.category());
        persistTransaction("Other", "100.00", LocalDate.of(2026,10,1), TransactionType.INCOME, other.account(), other.category());
        entityManager.flush();
        var balances = transactionRepository.balancesByUser(owner.account().getUser().getId(), TransactionType.INCOME);
        assertEquals(2, balances.size());
        assertEquals(new BigDecimal("199999999999999999.98"), balances.stream().filter(b -> b.accountId().equals(owner.account().getId())).findFirst().orElseThrow().balance());
        assertEquals(0, balances.stream().filter(b -> b.accountId().equals(empty.getId())).findFirst().orElseThrow().balance().signum());
    }

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

    @Test
    void shouldSumDecimalExpensesOnInclusivePeriodBoundaries() {
        TestData data = createTestData("expense-cents@email.com");
        persistTransaction("Primeiro dia", "0.10", LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE, data.account(), data.category());
        persistTransaction("Último dia", "0.20", LocalDate.of(2026, 10, 31),
            TransactionType.EXPENSE, data.account(), data.category());
        entityManager.flush();
        List<CategoryExpenseSummary> result = transactionRepository.sumExpensesByCategory(
            data.user().getId(), TransactionType.EXPENSE,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("0.30"), result.getFirst().amount());
    }

    @Test
    void shouldReturnNoCategoriesWhenPeriodHasNoExpenses() {
        TestData data = createTestData("expense-empty@email.com");
        persistTransaction("Outro mês", "10.00", LocalDate.of(2026, 9, 30),
            TransactionType.EXPENSE, data.account(), data.category());
        entityManager.flush();
        assertEquals(List.of(), transactionRepository.sumExpensesByCategory(
            data.user().getId(), TransactionType.EXPENSE,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
    }

    @Test
    void shouldPageFilteredRowsAndCountsWithoutLeakingOtherUsers() {
        TestData owner = createTestData("page-owner@email.com");
        TestData foreign = createTestData("page-foreign@email.com");
        for (int i = 1; i <= 3; i++) {
            persistTransaction("Página " + i, "10.00", LocalDate.of(2026, 10, 19), TransactionType.EXPENSE, owner.account(), owner.category());
        }
        persistTransaction("Receita", "100.00", LocalDate.of(2026, 10, 19), TransactionType.INCOME, owner.account(), owner.category());
        persistTransaction("Fora do período", "100.00", LocalDate.of(2026, 11, 1), TransactionType.EXPENSE, owner.account(), owner.category());
        persistTransaction("Outro usuário", "999.00", LocalDate.of(2026, 10, 19), TransactionType.EXPENSE, foreign.account(), foreign.category());
        Account otherAccount = new Account("Outra conta", owner.user());
        Category otherCategory = new Category("Outra categoria", owner.user());
        entityManager.persist(otherAccount);
        entityManager.persist(otherCategory);
        persistTransaction("Outra conta", "50.00", LocalDate.of(2026, 10, 19), TransactionType.EXPENSE, otherAccount, owner.category());
        persistTransaction("Outra categoria", "50.00", LocalDate.of(2026, 10, 19), TransactionType.EXPENSE, owner.account(), otherCategory);
        entityManager.flush();
        entityManager.clear();

        var first = transactionRepository.findPageByFilters(
            owner.user().getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
            TransactionType.EXPENSE, owner.account().getId(), owner.category().getId(), org.springframework.data.domain.PageRequest.of(0, 2)
        );
        var second = transactionRepository.findPageByFilters(
            owner.user().getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
            TransactionType.EXPENSE, owner.account().getId(), owner.category().getId(), org.springframework.data.domain.PageRequest.of(1, 2)
        );
        assertEquals(3, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(List.of("Página 3", "Página 2"), first.getContent().stream().map(Transaction::getDescription).toList());
        assertEquals(List.of("Página 1"), second.getContent().stream().map(Transaction::getDescription).toList());
        assertEquals(3, second.getTotalElements());
        assertEquals("Conta Teste", first.getContent().getFirst().getAccount().getName());
    }

    @Test
    void shouldReturnEmptyPageBeyondLastAndKeepCorrectTotal() {
        TestData owner = createTestData("page-empty@email.com");
        persistTransaction("Única", "10.00", LocalDate.of(2026, 10, 19), TransactionType.EXPENSE, owner.account(), owner.category());
        entityManager.flush();
        var result = transactionRepository.findPageByFilters(owner.user().getId(), null, null, null, null, null,
            org.springframework.data.domain.PageRequest.of(9, 10));
        assertEquals(0, result.getNumberOfElements());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
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
