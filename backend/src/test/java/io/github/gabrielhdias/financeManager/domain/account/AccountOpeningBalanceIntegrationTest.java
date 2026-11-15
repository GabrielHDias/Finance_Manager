package io.github.gabrielhdias.financeManager.domain.account;

import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.transaction.Transaction;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.integration.AbstractPostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountOpeningBalanceIntegrationTest extends AbstractPostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private io.github.gabrielhdias.financeManager.domain.user.UserRepository userRepository;

    @Autowired
    private io.github.gabrielhdias.financeManager.domain.category.CategoryRepository categoryRepository;

    @Autowired
    private io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository transactionRepository;

    @Test
    void openingBalanceIsStoredAndAddedToTransactionsWithoutAFakeEntry() throws Exception {
        User user = userRepository.saveAndFlush(new User("Saldo", "opening@example.test", "fixture-hash"));
        Account account = accountRepository.saveAndFlush(
            new Account("Carteira", new BigDecimal("-10.00"), user)
        );
        Category category = categoryRepository.saveAndFlush(new Category("Geral", user));
        transactionRepository.saveAndFlush(new Transaction(
            "Depósito",
            new BigDecimal("25.50"),
            LocalDate.of(2026, 11, 1),
            TransactionType.INCOME,
            account,
            category
        ));

        mockMvc.perform(get("/accounts/{id}", account.getId())
                .with(jwt().jwt(token -> token.subject(user.getId().toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openingBalance").value("-10.00"));

        mockMvc.perform(get("/accounts/{id}/balance", account.getId())
                .with(jwt().jwt(token -> token.subject(user.getId().toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value("15.50"));

        mockMvc.perform(get("/accounts/balances")
                .with(jwt().jwt(token -> token.subject(user.getId().toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].balance").value("15.50"));

        mockMvc.perform(get("/transactions")
                .with(jwt().jwt(token -> token.subject(user.getId().toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(post("/accounts")
                .with(jwt().jwt(token -> token.subject(user.getId().toString())))
                .contentType("application/json")
                .content("{\"name\":\"Outra\",\"openingBalance\":\"0\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.openingBalance").value("0.00"));
    }
}
