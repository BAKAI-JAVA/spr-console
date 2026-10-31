package kg.bakai.stopfactor.calculator;

import java.math.BigDecimal;
import java.util.List;

public record CheckedValue(Double number, List<String> values) {

    public static CheckedValue ofNumber(double number) {
        return new CheckedValue(number, List.of());
    }

    public static CheckedValue ofValues(List<String> values) {
        return new CheckedValue(null, List.copyOf(values));
    }

    public boolean isNumber() {
        return number != null;
    }

    public String asString() {
        if (number != null) {
            return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
        }
        return String.join(", ", values);
    }
}
