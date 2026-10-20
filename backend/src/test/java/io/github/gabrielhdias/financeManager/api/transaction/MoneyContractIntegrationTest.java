package io.github.gabrielhdias.financeManager.api.transaction;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.integration.AbstractPostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MoneyContractIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Test void openApiAdvertisesTheActualDecimalStringContract() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.components.schemas.TransactionResponse.properties.amount.type").value("string"))
            .andExpect(jsonPath("$.components.schemas.TransactionRequest.properties.amount.type").value("string"))
            .andExpect(jsonPath("$.components.schemas.BudgetSummary.properties.spent.type").value("string"))
            .andExpect(jsonPath("$.components.schemas.AccountBalanceResponse.properties.balance.type").value("string"));
    }
    @Test void maximumAndHighTotalsRoundTripWithoutRounding() throws Exception {
        User owner = new User("Money", "money-contract@example.test", "fixture-hash"); em.persist(owner);
        Account account = new Account("Money",owner); Category category = new Category("Money",owner);
        em.persist(account); em.persist(category); em.flush();
        for (int i=0;i<2;i++) {
            mvc.perform(post("/transactions").with(jwt().jwt(token -> token.subject(owner.getId().toString())))
                .contentType("application/json").content("{\"description\":\"Maximum\",\"amount\":\"99999999999999999.99\",\"date\":\"2026-10-20\",\"type\":\"INCOME\",\"accountId\":"+account.getId()+",\"categoryId\":"+category.getId()+"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amount").isString())
                .andExpect(jsonPath("$.amount").value("99999999999999999.99"));
        }
        mvc.perform(get("/dashboard/summary").with(jwt().jwt(token -> token.subject(owner.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalIncome").value("199999999999999999.98"))
            .andExpect(jsonPath("$.totalExpense").value("0.00"));
        mvc.perform(get("/accounts/balances").with(jwt().jwt(token -> token.subject(owner.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].balance").value("199999999999999999.98"));
    }
}
