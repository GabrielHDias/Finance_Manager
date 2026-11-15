package io.github.gabrielhdias.financeManager.domain.tag;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
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
class TagIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired io.github.gabrielhdias.financeManager.domain.user.UserRepository users;

    @Test
    void tagsFilterTransactionsAndGroupTheChartWithoutDoubleCountingTheTotal() throws Exception {
        User user = users.saveAndFlush(new User("Etiquetas", "tags@example.test", "fixture-hash"));
        String subject = user.getId().toString();
        long accountId = id(mockMvc.perform(post("/accounts").with(jwt().jwt(token -> token.subject(subject)))
            .contentType("application/json").content("{\"name\":\"Conta\"}")).andReturn().getResponse().getContentAsString());
        long categoryId = id(mockMvc.perform(post("/categories").with(jwt().jwt(token -> token.subject(subject)))
            .contentType("application/json").content("{\"name\":\"Mercado\",\"type\":\"EXPENSE\"}")).andReturn().getResponse().getContentAsString());
        long work = id(mockMvc.perform(post("/tags").with(jwt().jwt(token -> token.subject(subject)))
            .contentType("application/json").content("{\"name\":\"Trabalho\"}")).andReturn().getResponse().getContentAsString());
        long home = id(mockMvc.perform(post("/tags").with(jwt().jwt(token -> token.subject(subject)))
            .contentType("application/json").content("{\"name\":\"Casa\"}")).andReturn().getResponse().getContentAsString());
        mockMvc.perform(post("/transactions").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"description\":\"Ambos\",\"amount\":\"10.00\",\"date\":\"2026-11-01\",\"type\":\"EXPENSE\",\"accountId\":"
                    + accountId + ",\"categoryId\":" + categoryId + ",\"tagIds\":[" + work + "," + home + "]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tags.length()").value(2));
        mockMvc.perform(get("/transactions?tagId=" + work).with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/dashboard/transactions-summary?type=EXPENSE&groupBy=TAG&startDate=2026-11-01&endDate=2026-11-30")
                .with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(jsonPath("$.total").value("10.00"))
            .andExpect(jsonPath("$.items.length()").value(2));
        mockMvc.perform(post("/tags").with(jwt().jwt(token -> token.subject(user.getId().toString() + "9")))
                .contentType("application/json").content("{\"name\":\"Alheia\"}"))
            .andExpect(status().isNotFound());
    }

    private static long id(String json) {
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }
}
