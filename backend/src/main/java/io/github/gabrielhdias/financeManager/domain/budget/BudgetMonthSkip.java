package io.github.gabrielhdias.financeManager.domain.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "budget_month_skips")
public class BudgetMonthSkip {
    @EmbeddedId
    private Key id;

    protected BudgetMonthSkip() {}

    public BudgetMonthSkip(Long categoryId, LocalDate periodStart) {
        this.id = new Key(categoryId, periodStart);
    }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "category_id")
        private Long categoryId;
        @Column(name = "period_start")
        private LocalDate periodStart;

        protected Key() {}

        public Key(Long categoryId, LocalDate periodStart) {
            this.categoryId = categoryId;
            this.periodStart = periodStart;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(categoryId, key.categoryId) && Objects.equals(periodStart, key.periodStart);
        }

        @Override
        public int hashCode() {
            return Objects.hash(categoryId, periodStart);
        }
    }
}
