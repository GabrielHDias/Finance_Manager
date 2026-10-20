package io.github.gabrielhdias.financeManager.api.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@RestController
public class HealthController {
    private final DataSource dataSource;
    public HealthController(DataSource dataSource) { this.dataSource = dataSource; }
    @GetMapping("/health/live")
    public Map<String, String> live() { return Map.of("status", "UP"); }
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, String>> ready() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) return ResponseEntity.ok(Map.of("status", "UP"));
        } catch (SQLException unavailable) { /* No database diagnostics on the public endpoint. */ }
        return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
    }
}
