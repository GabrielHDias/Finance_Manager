package io.github.gabrielhdias.financeManager.api.money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.core.JsonGenerator;
public final class MoneySerializer extends ValueSerializer<BigDecimal> {
    @Override public void serialize(BigDecimal value, JsonGenerator generator, SerializationContext context) {
        generator.writeString(value.setScale(2, RoundingMode.UNNECESSARY).toPlainString());
    }
}
