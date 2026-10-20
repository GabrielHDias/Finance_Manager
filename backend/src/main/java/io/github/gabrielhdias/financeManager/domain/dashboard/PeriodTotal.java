package io.github.gabrielhdias.financeManager.domain.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PeriodTotal(LocalDate date, BigDecimal amount) {}
