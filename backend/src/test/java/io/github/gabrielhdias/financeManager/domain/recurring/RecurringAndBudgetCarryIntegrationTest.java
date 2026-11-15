package io.github.gabrielhdias.financeManager.domain.recurring;

import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.integration.AbstractPostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecurringAndBudgetCarryIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired io.github.gabrielhdias.financeManager.domain.user.UserRepository users;

    @Test
    void monthlyRuleGeneratesOnceAndBudgetRepeatsUntilTheMonthIsSkipped() throws Exception {
        User user = users.saveAndFlush(new User("Recorrente", "recurring@example.test", "fixture-hash"));
        String subject = user.getId().toString();
        long accountId = id(body(mockMvc.perform(post("/accounts").with(jwt().jwt(token -> token.subject(subject)))
            .contentType("application/json").content("{\"name\":\"Conta\"}")).andExpect(status().isCreated())));
        long categoryId = id(body(mockMvc.perform(post("/categories").with(jwt().jwt(token -> token.subject(subject)))
            .contentType("application/json").content("{\"name\":\"Aluguel\",\"type\":\"EXPENSE\"}")).andExpect(status().isCreated())));
        mockMvc.perform(post("/recurring-rules").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"description\":\"Aluguel\",\"amount\":\"100.00\",\"type\":\"EXPENSE\",\"frequency\":\"MONTHLY\",\"startDate\":\"2026-10-01\",\"accountId\":"
                    + accountId + ",\"categoryId\":" + categoryId + "}"))
            .andExpect(status().isCreated());
        mockMvc.perform(get("/transactions").with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/transactions").with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(jsonPath("$.length()").value(2));
        String firstId = mockMvc.perform(get("/transactions").with(jwt().jwt(token -> token.subject(subject))))
            .andReturn().getResponse().getContentAsString().replaceAll(".*?\"id\":(\\d+).*", "$1");
        mockMvc.perform(delete("/transactions/" + firstId).with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/transactions").with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(jsonPath("$.length()").value(1));

        String budget = body(mockMvc.perform(put("/budgets").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"categoryId\":" + categoryId + ",\"year\":2026,\"month\":10,\"limitAmount\":\"80.00\",\"repeatsMonthly\":true}"))
            .andExpect(status().isOk()));
        mockMvc.perform(get("/budgets?year=2026&month=11").with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].limitAmount").value("80.00"));
        String novemberId = mockMvc.perform(get("/budgets?year=2026&month=11").with(jwt().jwt(token -> token.subject(subject))))
            .andReturn().getResponse().getContentAsString().replaceAll(".*?\"id\":(\\d+).*", "$1");
        mockMvc.perform(delete("/budgets/" + novemberId).with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/budgets?year=2026&month=11").with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(jsonPath("$.length()").value(0));
        org.junit.jupiter.api.Assertions.assertTrue(budget.contains("80.00"));
    }

    private static String body(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }

    private static long id(String json) {
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }
}
