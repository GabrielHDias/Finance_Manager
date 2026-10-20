package io.github.gabrielhdias.financeManager.api.health;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
class HealthControllerTest {
    @Test void readinessFailsAndRecoversWithoutAffectingLiveness() throws Exception {
        DataSource source = mock(DataSource.class); Connection connection = mock(Connection.class);
        when(source.getConnection()).thenThrow(new SQLException("private diagnostics")).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        var health = new HealthController(source);
        assertThat(health.live()).containsEntry("status","UP"); verifyNoInteractions(source);
        assertThat(health.ready().getStatusCode().value()).isEqualTo(503);
        assertThat(health.live()).containsEntry("status","UP");
        assertThat(health.ready().getStatusCode().value()).isEqualTo(200);
        verify(connection).close();
    }
}
