package io.github.gabrielhdias.financeManager.config.security;

import io.github.gabrielhdias.financeManager.api.account.AccountController;
import io.github.gabrielhdias.financeManager.domain.account.AccountService;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityTestConfig.class)
class SecurityErrorResponseMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnStandardApiErrorWhenAuthenticationIsMissing()
        throws Exception {

        mockMvc.perform(
                get("/accounts")
            )
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.status")
                    .value(401)
            )
            .andExpect(
                jsonPath("$.error")
                    .value("Unauthorized")
            )
            .andExpect(
                jsonPath("$.message")
                    .value("Autenticação necessária")
            )
            .andExpect(
                jsonPath("$.path")
                    .value("/accounts")
            )
            .andExpect(
                jsonPath("$.fields")
                    .isMap()
            )
            .andExpect(
                jsonPath("$.timestamp")
                    .exists()
            );
    }
}
