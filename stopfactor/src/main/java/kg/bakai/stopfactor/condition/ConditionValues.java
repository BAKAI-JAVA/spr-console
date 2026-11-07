package kg.bakai.stopfactor.condition;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConditionValues {

    private ConditionValues() {
    }

    public static BigDecimal parseNumber(String value) {
        return new BigDecimal(value.trim());
    }

    public static Set<String> parseItems(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ConditionValues::normalizeItem)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String normalizeItem(String item) {
        return item.trim().toLowerCase(Locale.ROOT);
    }
}
