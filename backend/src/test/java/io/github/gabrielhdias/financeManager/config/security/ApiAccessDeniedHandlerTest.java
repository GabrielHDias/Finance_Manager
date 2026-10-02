package io.github.gabrielhdias.financeManager.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper =
        JsonMapper.builder()
            .findAndAddModules()
            .build();

    private final ApiAccessDeniedHandler accessDeniedHandler =
        new ApiAccessDeniedHandler(objectMapper);

    @Test
    void shouldReturnStandardApiErrorWhenAccessIsDenied()
        throws Exception {

        MockHttpServletRequest request =
            new MockHttpServletRequest();

        request.setRequestURI("/accounts");

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        AccessDeniedException exception =
            new AccessDeniedException(
                "Access Denied"
            );

        accessDeniedHandler.handle(
            request,
            response,
            exception
        );

        String body =
            response.getContentAsString();

        assertThat(response.getStatus())
            .isEqualTo(
                HttpStatus.FORBIDDEN.value()
            );

        assertThat(response.getContentType())
            .startsWith("application/json");

        assertThat(body)
            .contains("\"status\":403");

        assertThat(body)
            .contains("\"error\":\"Forbidden\"");

        assertThat(body)
            .contains("\"message\":\"Acesso negado\"");

        assertThat(body)
            .contains("\"path\":\"/accounts\"");

        assertThat(body)
            .contains("\"fields\":{}");
    }
}
