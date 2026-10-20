package io.github.gabrielhdias.financeManager.api.budget;

import io.github.gabrielhdias.financeManager.domain.budget.BudgetService;
import io.github.gabrielhdias.financeManager.domain.budget.BudgetSummary;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.annotation.JsonSerialize;
import io.github.gabrielhdias.financeManager.api.money.MoneySerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@RestController
@RequestMapping("/budgets")
@Tag(name = "Budgets", description = "Orçamentos mensais por categoria")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {
    private final BudgetService service;
    private final AuthenticatedUser user;

    public BudgetController(BudgetService service, AuthenticatedUser user) {
        this.service = service;
        this.user = user;
    }

    @GetMapping
    public List<BudgetSummary> list(@RequestParam(defaultValue = "0") int year, @RequestParam(defaultValue = "0") int month) {
        return service.list(user.getId(), year, month);
    }

    @PutMapping
    public BudgetSummary save(@Valid @RequestBody BudgetRequest request) {
        return service.save(user.getId(), request.categoryId(), request.year(), request.month(), request.limitAmount());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    public record BudgetRequest(
        @NotNull(message = "Informe a categoria") @Positive Long categoryId,
        @Min(1) @Max(9999) int year,
        @Min(1) @Max(12) int month,
        @NotNull(message = "Informe o limite") @Positive(message = "O limite deve ser positivo")
        @Digits(integer = 17, fraction = 2, message = "Use até duas casas decimais") @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonSerialize(using = MoneySerializer.class) @Schema(type = "string", implementation = String.class, example = "0.30") BigDecimal limitAmount
    ) {}
}
