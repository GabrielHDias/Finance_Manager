package io.github.gabrielhdias.financeManager.api.budget;

import io.github.gabrielhdias.financeManager.config.security.SecurityTestConfig;
import io.github.gabrielhdias.financeManager.domain.budget.BudgetService;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BudgetController.class)
@Import(SecurityTestConfig.class)
class BudgetControllerMvcTest {
    @Autowired MockMvc mvc;
    @MockitoBean BudgetService service;
    @MockitoBean AuthenticatedUser user;
    @MockitoBean JwtDecoder decoder;

    @Test
    void shouldProtectAllOperations() throws Exception {
        mvc.perform(get("/budgets?year=2026&month=10")).andExpect(status().isUnauthorized());
        mvc.perform(put("/budgets").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/budgets/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPassAuthenticatedOwnerAndPeriod() throws Exception {
        when(user.getId()).thenReturn(7L);
        when(service.list(7L, 2026, 10)).thenReturn(List.of());
        mvc.perform(get("/budgets?year=2026&month=10").with(jwt())).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).list(7L, 2026, 10);
        mvc.perform(delete("/budgets/5").with(jwt())).andExpect(status().isNoContent());
        verify(service).delete(7L, 5L);
    }

    @Test
    void shouldRejectInvalidLimitAndPeriodBeforeWriting() throws Exception {
        mvc.perform(put("/budgets").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":1,\"year\":0,\"month\":13,\"limitAmount\":-1}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.limitAmount").exists());
        mvc.perform(put("/budgets").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":1,\"year\":2026,\"month\":10,\"limitAmount\":1.001}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
