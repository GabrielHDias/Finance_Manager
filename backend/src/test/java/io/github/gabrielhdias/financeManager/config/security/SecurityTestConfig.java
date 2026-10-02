package io.github.gabrielhdias.financeManager.config.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
    SecurityConfig.class,
    ApiAuthenticationEntryPoint.class,
    ApiAccessDeniedHandler.class
})
public class SecurityTestConfig {
}
