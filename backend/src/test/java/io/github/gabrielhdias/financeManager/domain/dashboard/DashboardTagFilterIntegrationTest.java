package io.github.gabrielhdias.financeManager.domain.dashboard;

import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.integration.AbstractPostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardTagFilterIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired io.github.gabrielhdias.financeManager.domain.user.UserRepository users;

    @Test
    void summaryComparisonAndChartUseOnlyTheSelectedTag() throws Exception {
        User user = users.saveAndFlush(new User("Painel", "dashboard-tag@example.test", "fixture-hash"));
        String subject = user.getId().toString();
        long accountId = id(mockMvc.perform(post("/accounts").with(jwt().jwt(token -> token.subject(subject)))
            .contentType("application/json").content("{\"name\":\"Conta\"}")).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString());
        long expenseId = id(mockMvc.perform(post("/categories").with(jwt().jwt(token -> token.subject(subject)))
            .contentType("application/json").content("{\"name\":\"Mercado\",\"type\":\"EXPENSE\"}")).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString());
        long incomeId = id(mockMvc.perform(post("/categories").with(jwt().jwt(token -> token.subject(subject)))
            .contentType("application/json").content("{\"name\":\"Salário\",\"type\":\"INCOME\"}")).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString());
        long work = id(mockMvc.perform(post("/tags").with(jwt().jwt(token -> token.subject(subject)))
            .contentType("application/json").content("{\"name\":\"Trabalho\"}")).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString());
        postTransaction(subject, "Marcado", "10.00", "EXPENSE", "2026-11-02", accountId, expenseId, work);
        postTransaction(subject, "Sem etiqueta", "40.00", "EXPENSE", "2026-11-03", accountId, expenseId, null);
        postTransaction(subject, "Receita marcada", "25.00", "INCOME", "2026-10-15", accountId, incomeId, work);
        postTransaction(subject, "Receita solta", "5.00", "INCOME", "2026-10-20", accountId, incomeId, null);

        mockMvc.perform(get("/dashboard/summary").param("startDate", "2026-10-01").param("endDate", "2026-11-30")
                .with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalIncome").value("30.00"))
            .andExpect(jsonPath("$.totalExpense").value("50.00"))
            .andExpect(jsonPath("$.balance").value("-20.00"));
        mockMvc.perform(get("/dashboard/summary").param("startDate", "2026-11-01").param("endDate", "2026-11-30")
                .param("tagId", String.valueOf(work)).with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalIncome").value("0.00"))
            .andExpect(jsonPath("$.totalExpense").value("10.00"))
            .andExpect(jsonPath("$.balance").value("-10.00"));
        mockMvc.perform(get("/dashboard/summary").param("startDate", "2026-10-01").param("endDate", "2026-10-31")
                .param("tagId", String.valueOf(work)).with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(jsonPath("$.totalIncome").value("25.00"))
            .andExpect(jsonPath("$.totalExpense").value("0.00"));
        mockMvc.perform(get("/dashboard/transactions-summary").param("type", "EXPENSE").param("groupBy", "DAY")
                .param("startDate", "2026-11-01").param("endDate", "2026-11-03").param("tagId", String.valueOf(work))
                .with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value("10.00"))
            .andExpect(jsonPath("$.items[1].total").value("10.00"))
            .andExpect(jsonPath("$.items[2].total").value("0.00"));
    }

    private void postTransaction(String subject, String description, String amount, String type, String date,
                      long accountId, long categoryId, Long tagId) throws Exception {
        String tags = tagId == null ? "" : ",\"tagIds\":[" + tagId + "]";
        mockMvc.perform(post("/transactions").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"description\":\"" + description + "\",\"amount\":\"" + amount + "\",\"date\":\"" + date
                    + "\",\"type\":\"" + type + "\",\"accountId\":" + accountId + ",\"categoryId\":" + categoryId + tags + "}"))
            .andExpect(status().isCreated());
    }

    private static long id(String json) {
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }
}
