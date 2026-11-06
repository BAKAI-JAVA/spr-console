package kg.bakai.stopfactor.calculator;

import java.util.List;

/**
 * What a calculator measured: a number, or the loan statuses of the client (values).
 */
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
}
