package io.github.gabrielhdias.financeManager.domain.transaction;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.account.AccountRepository;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryRepository;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserRepository;
import io.github.gabrielhdias.financeManager.integration.AbstractPostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransferIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired TransactionRepository transactions;
    @Autowired io.github.gabrielhdias.financeManager.domain.dashboard.DashboardService dashboard;

    @Test
    void transferMovesBothBalancesAndStaysOutOfIncomeAndExpense() throws Exception {
        User user = users.saveAndFlush(new User("Transfere", "transfer@example.test", "fixture-hash"));
        Account source = accounts.saveAndFlush(new Account("Origem", new BigDecimal("100.00"), user));
        Account target = accounts.saveAndFlush(new Account("Destino", new BigDecimal("5.00"), user));
        Category expense = categories.saveAndFlush(new Category("Mercado", user));
        transactions.saveAndFlush(new Transaction("Compra", new BigDecimal("10.00"), java.time.LocalDate.of(2026, 11, 2),
            TransactionType.EXPENSE, source, expense));

        String subject = user.getId().toString();
        mockMvc.perform(post("/transactions").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"description\":\"Mesada\",\"amount\":\"40.00\",\"date\":\"2026-11-03\",\"type\":\"TRANSFER\",\"accountId\":"
                    + source.getId() + ",\"destinationAccountId\":" + target.getId() + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("TRANSFER"))
            .andExpect(jsonPath("$.categoryId").value(nullValue()))
            .andExpect(jsonPath("$.destinationAccountName").value("Destino"));

        mockMvc.perform(get("/accounts/" + source.getId() + "/balance").with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(jsonPath("$.balance").value("50.00"));
        mockMvc.perform(get("/accounts/" + target.getId() + "/balance").with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(jsonPath("$.balance").value("45.00"));

        var summary = dashboard.getSummary(user.getId(), null, null);
        assertEquals(0, summary.totalIncome().compareTo(BigDecimal.ZERO));
        assertEquals(0, summary.totalExpense().compareTo(new BigDecimal("10.00")));

        mockMvc.perform(get("/transactions").with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.type == 'TRANSFER')].description").value("Mesada"));

        mockMvc.perform(post("/transactions").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"description\":\"Mesma\",\"amount\":\"1.00\",\"date\":\"2026-11-03\",\"type\":\"TRANSFER\",\"accountId\":"
                    + source.getId() + ",\"destinationAccountId\":" + source.getId() + "}"))
            .andExpect(status().isConflict());
    }
}
