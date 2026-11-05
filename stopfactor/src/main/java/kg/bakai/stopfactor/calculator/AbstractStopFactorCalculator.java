package kg.bakai.stopfactor.calculator;

import java.math.BigDecimal;
import java.util.*;

/**
 * Param normalization shared by the calculators: unknown keys, required params, integers and their minimum,
 * values from a fixed list.
 */
public abstract class AbstractStopFactorCalculator<P> implements StopFactorCalculator<P> {

    @Override
    public Map<String, Object> normalizeParams(Map<String, Object> params) {
        Map<String, Object> source = params != null ? params : Map.of();
        List<String> errors = new ArrayList<>();
        Map<String, ParamSpec> specs = new LinkedHashMap<>();
        paramSpecs().forEach(spec -> specs.put(spec.key(), spec));

        source.keySet().stream()
                .filter(key -> !specs.containsKey(key))
                .forEach(key -> errors.add("Неизвестный параметр '" + key + "'"));

        Map<String, Object> normalized = new LinkedHashMap<>();
        for (ParamSpec spec : specs.values()) {
            Object raw = source.get(spec.key());
            if (raw == null || (raw instanceof String s && s.isBlank())) {
                if (spec.required()) {
                    errors.add("Параметр '" + spec.key() + "' обязателен");
                }
                continue;
            }
            if (spec.isInteger()) {
                Integer value = toInteger(raw);
                if (value == null) {
                    errors.add("Параметр '" + spec.key() + "' должен быть целым числом");
                } else if (value < spec.min()) {
                    errors.add("Параметр '" + spec.key() + "' должен быть >= " + spec.min());
                } else {
                    normalized.put(spec.key(), value);
                }
            } else {
                String value = raw.toString().trim();
                if (!spec.allowedValues().contains(value)) {
                    errors.add("Параметр '" + spec.key() + "' должен быть одним из " + new TreeSet<>(spec.allowedValues()));
                } else {
                    normalized.put(spec.key(), value);
                }
            }
        }
        if (errors.isEmpty()) {
            validateNormalized(normalized, errors);
        }
        if (!errors.isEmpty()) {
            throw new InvalidParamsException(errors);
        }
        return normalized;
    }

    protected void validateNormalized(Map<String, Object> params, List<String> errors) {
    }

    protected static void requireOrdered(Map<String, Object> params, String fromKey, String toKey, List<String> errors) {
        Integer from = (Integer) params.get(fromKey);
        Integer to = (Integer) params.get(toKey);
        if (from != null && to != null && from > to) {
            errors.add("Параметр '" + fromKey + "' должен быть <= '" + toKey + "'");
        }
    }

    protected static int intParam(Map<String, Object> params, String key) {
        Integer value = toInteger(params.get(key));
        if (value == null) {
            throw new IllegalStateException("Параметр стоп-фактора '" + key + "' отсутствует или не является целым числом: " + params);
        }
        return value;
    }

    protected static Integer optionalIntParam(Map<String, Object> params, String key) {
        Object raw = params.get(key);
        if (raw == null) {
            return null;
        }
        Integer value = toInteger(raw);
        if (value == null) {
            throw new IllegalStateException("Параметр стоп-фактора '" + key + "' не является целым числом: " + params);
        }
        return value;
    }

    protected static String stringParam(Map<String, Object> params, String key) {
        Object raw = params.get(key);
        if (raw == null) {
            throw new IllegalStateException("Параметр стоп-фактора '" + key + "' отсутствует: " + params);
        }
        return raw.toString();
    }

    private static Integer toInteger(Object raw) {
        try {
            BigDecimal number = switch (raw) {
                case Integer i -> BigDecimal.valueOf(i);
                case Long l -> BigDecimal.valueOf(l);
                case Short s -> BigDecimal.valueOf(s);
                case BigDecimal d -> d;
                case Number n -> new BigDecimal(n.toString());
                case String s -> new BigDecimal(s.trim());
                default -> null;
            };
            return number == null ? null : number.intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }
}
