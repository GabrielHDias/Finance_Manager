package io.github.gabrielhdias.financeManager.domain.transaction;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        User user = new User(
            "Gabriel",
            "repository-test@email.com",
            "$2a$10$encodedPassword"
        );

        entityManager.persist(user);

        Account account = new Account(
            "Conta Teste",
            user
        );

        entityManager.persist(account);

        Category incomeCategory = new Category(
            "Receitas",
            user
        );

        Category expenseCategory = new Category(
            "Despesas",
            user
        );

        entityManager.persist(incomeCategory);
        entityManager.persist(expenseCategory);

        Transaction income = new Transaction(
            "Salário",
            new BigDecimal("1000.00"),
            LocalDate.of(2026, 10, 1),
            TransactionType.INCOME,
            account,
            incomeCategory
        );

        Transaction expenseOne = new Transaction(
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            account,
            expenseCategory
        );

        Transaction expenseTwo = new Transaction(
            "Uber",
            new BigDecimal("35.90"),
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            account,
            expenseCategory
        );

        entityManager.persist(income);
        entityManager.persist(expenseOne);
        entityManager.persist(expenseTwo);

        entityManager.flush();

        BigDecimal balance =
            transactionRepository.calculateBalanceByAccountId(
                account.getId(),
                TransactionType.INCOME
            );

        assertEquals(
            new BigDecimal("814.10"),
            balance
        );
    }

    @Test
    void shouldReturnZeroWhenAccountHasNoTransactions() {
        User user = new User(
            "Gabriel",
            "repository-empty@email.com",
            "$2a$10$encodedPassword"
        );

        entityManager.persist(user);

        Account account = new Account(
            "Conta Sem Transações",
            user
        );

        entityManager.persist(account);

        entityManager.flush();

        BigDecimal balance =
            transactionRepository.calculateBalanceByAccountId(
                account.getId(),
                TransactionType.INCOME
            );

        assertEquals(
            0,
            balance.compareTo(BigDecimal.ZERO)
        );
    }
}
