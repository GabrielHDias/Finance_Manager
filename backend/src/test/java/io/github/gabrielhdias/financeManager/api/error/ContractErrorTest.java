package io.github.gabrielhdias.financeManager.api.error;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.assertThat;
class ContractErrorTest {
    @Test void databaseConstraintsAreMappedSpecificallyWithoutPrivateDetails() {
        var handler = new GlobalExceptionHandler(); var request = new MockHttpServletRequest("POST","/users");
        for (String name : new String[]{"uk_users_email","uk_accounts_user_name","uk_categories_user_name","uk_budgets_category_month"}) {
            var violation = new ConstraintViolationException("private SQL",new SQLException("secret"),name);
            var result = handler.integrity(new DataIntegrityViolationException("private",violation),request);
            assertThat(result.getStatusCode().value()).isEqualTo(409);
            assertThat(result.getBody().message()).doesNotContain("secret","private",name);
        }
        assertThat(handler.integrity(new DataIntegrityViolationException("unknown"),request).getStatusCode().value()).isEqualTo(500);
        assertThat(handler.malformed(request).getBody().timestamp().toString()).endsWith("Z");
    }
}
