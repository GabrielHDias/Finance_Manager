package io.github.gabrielhdias.financeManager.config.security;

import io.github.gabrielhdias.financeManager.api.account.AccountController;
import io.github.gabrielhdias.financeManager.domain.account.AccountService;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = AccountController.class,
    properties = {
        "app.cors.allowed-origin=http://localhost:3000"
    }
)
@Import(SecurityConfig.class)
class SecurityCorsMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldAllowConfiguredOrigin() throws Exception {
        mockMvc.perform(
                options("/accounts")
                    .header(
                        HttpHeaders.ORIGIN,
                        "http://localhost:3000"
                    )
                    .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                        HttpMethod.GET.name()
                    )
                    .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                        HttpHeaders.AUTHORIZATION
                    )
            )
            .andExpect(status().isOk())
            .andExpect(
                header().string(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                    "http://localhost:3000"
                )
            )
            .andExpect(
                header().string(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                    "GET,POST,PUT,DELETE,OPTIONS"
                )
            )
            .andExpect(
                header().string(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    HttpHeaders.AUTHORIZATION
                )
            );
    }

    @Test
    void shouldRejectNonConfiguredOrigin() throws Exception {
        mockMvc.perform(
                options("/accounts")
                    .header(
                        HttpHeaders.ORIGIN,
                        "http://localhost:9999"
                    )
                    .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                        HttpMethod.GET.name()
                    )
            )
            .andExpect(status().isForbidden())
            .andExpect(
                header().doesNotExist(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
                )
            );
    }
}
