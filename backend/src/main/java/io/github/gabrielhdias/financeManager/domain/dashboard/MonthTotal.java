package io.github.gabrielhdias.financeManager.domain.dashboard;

import java.math.BigDecimal;

public record MonthTotal(Integer year, Integer month, BigDecimal amount) {}
