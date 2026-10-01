package io.github.gabrielhdias.financeManager.api.account;

import io.github.gabrielhdias.financeManager.config.security.SecurityConfig;
import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.account.AccountService;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnUnauthorizedWhenRequestHasNoToken() throws Exception {
        mockMvc.perform(
                get("/accounts")
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateAccountWhenAuthenticated() throws Exception {
        Long userId = 1L;

        Account account = mock(Account.class);

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(account.getId())
            .thenReturn(10L);

        when(account.getName())
            .thenReturn("Nubank");

        when(accountService.create(
            userId,
            "Nubank"
        )).thenReturn(account);

        mockMvc.perform(
                post("/accounts")
                    .with(jwt().jwt(jwt ->
                        jwt.subject(userId.toString())
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Nubank"
                                }
                                """)
            )
            .andExpect(status().isCreated())
            .andExpect(
                content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.name").value("Nubank"));
    }

    @Test
    void shouldReturnBadRequestWhenAccountNameIsBlank() throws Exception {
        when(authenticatedUser.getId())
            .thenReturn(1L);

        mockMvc.perform(
                post("/accounts")
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": ""
                                }
                                """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error")
                .value("Validation Error"))
            .andExpect(jsonPath("$.message")
                .value("Dados inválidos"))
            .andExpect(jsonPath("$.fields.name").exists());
    }
}
