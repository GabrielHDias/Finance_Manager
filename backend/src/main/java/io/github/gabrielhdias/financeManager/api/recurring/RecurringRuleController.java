package io.github.gabrielhdias.financeManager.api.recurring;

import io.github.gabrielhdias.financeManager.domain.recurring.RecurringFrequency;
import io.github.gabrielhdias.financeManager.domain.recurring.RecurringRule;
import io.github.gabrielhdias.financeManager.domain.recurring.RecurringRuleService;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/recurring-rules")
@SecurityRequirement(name = "bearerAuth")
public class RecurringRuleController {
    private final RecurringRuleService service;
    private final AuthenticatedUser user;

    public RecurringRuleController(RecurringRuleService service, AuthenticatedUser user) {
        this.service = service;
        this.user = user;
    }

    @GetMapping
    public List<RecurringRuleResponse> list() {
        return service.list(user.getId()).stream().map(RecurringRuleResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<RecurringRuleResponse> create(@Valid @RequestBody RecurringRuleRequest request) {
        RecurringRule rule = service.create(user.getId(), request.description(), request.amount(), request.type(),
            request.frequency(), request.startDate(), request.endDate(), request.accountId(), request.categoryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(RecurringRuleResponse.from(rule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    public record RecurringRuleRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull TransactionType type,
        @NotNull RecurringFrequency frequency,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull @Positive Long accountId,
        @NotNull @Positive Long categoryId
    ) {}

    public record RecurringRuleResponse(Long id, String description, BigDecimal amount, TransactionType type,
                                        RecurringFrequency frequency, LocalDate startDate, LocalDate endDate,
                                        Long accountId, String accountName, Long categoryId, String categoryName) {
        static RecurringRuleResponse from(RecurringRule rule) {
            return new RecurringRuleResponse(rule.getId(), rule.getDescription(), rule.getAmount().setScale(2), rule.getType(),
                rule.getFrequency(), rule.getStartDate(), rule.getEndDate(), rule.getAccount().getId(),
                rule.getAccount().getName(), rule.getCategory().getId(), rule.getCategory().getName());
        }
    }
}
