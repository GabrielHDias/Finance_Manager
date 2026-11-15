package io.github.gabrielhdias.financeManager.domain.category;

import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.integration.AbstractPostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryTypeIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired io.github.gabrielhdias.financeManager.domain.user.UserRepository users;

    @Test
    void transactionAndBudgetFollowTheCategoryType() throws Exception {
        User user = users.saveAndFlush(new User("Tipos", "types@example.test", "fixture-hash"));
        String subject = user.getId().toString();
        String account = mockMvc.perform(post("/accounts").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json").content("{\"name\":\"Conta\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long accountId = Long.parseLong(account.replaceAll(".*\"id\":(\\d+).*", "$1"));
        String expense = mockMvc.perform(post("/categories").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json").content("{\"name\":\"Mercado\",\"type\":\"EXPENSE\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("EXPENSE"))
            .andReturn().getResponse().getContentAsString();
        long expenseId = Long.parseLong(expense.replaceAll(".*\"id\":(\\d+).*", "$1"));
        String income = mockMvc.perform(post("/categories").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json").content("{\"name\":\"Salário\",\"type\":\"INCOME\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("INCOME"))
            .andReturn().getResponse().getContentAsString();
        long incomeId = Long.parseLong(income.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(post("/transactions").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"description\":\"Errada\",\"amount\":\"10.00\",\"date\":\"2026-11-01\",\"type\":\"INCOME\",\"accountId\":" + accountId + ",\"categoryId\":" + expenseId + "}"))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/transactions").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"description\":\"Certa\",\"amount\":\"10.00\",\"date\":\"2026-11-01\",\"type\":\"INCOME\",\"accountId\":" + accountId + ",\"categoryId\":" + incomeId + "}"))
            .andExpect(status().isCreated());
        mockMvc.perform(put("/budgets").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"categoryId\":" + incomeId + ",\"year\":2026,\"month\":11,\"limitAmount\":\"20.00\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(put("/budgets").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"categoryId\":" + expenseId + ",\"year\":2026,\"month\":11,\"limitAmount\":\"20.00\"}"))
            .andExpect(status().isOk());
    }
}
