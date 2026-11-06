package kg.bakai.stopfactor.service.console.impl;

import kg.bakai.stopfactor.calculator.CalculatorRegistry;
import kg.bakai.stopfactor.calculator.InvalidParamsException;
import kg.bakai.stopfactor.calculator.StopFactorCalculator;
import kg.bakai.stopfactor.calculator.ValueKind;
import kg.bakai.stopfactor.dto.console.MetricParamSchema;
import kg.bakai.stopfactor.dto.console.ConditionDto;
import kg.bakai.stopfactor.entity.StopFactorMetric;
import kg.bakai.stopfactor.enums.ConditionOperator;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;
import kg.bakai.stopfactor.evaluation.ConditionValues;
import kg.bakai.stopfactor.repository.StopFactorRepository;
import kg.bakai.stopfactor.repository.projection.ProductProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class StopFactorDefinitionValidator {

    private static final Set<StopFactorStatus> CONDITION_RESULTS = EnumSet.of(StopFactorStatus.REJECTED, StopFactorStatus.UNDERWRITER);

    private final CalculatorRegistry calculatorRegistry;
    private final StopFactorRepository stopFactorRepository;

    Map<String, Object> validateParams(StopFactorMetric metric, Map<String, Object> params, List<String> errors) {
        Map<String, Object> source = params != null ? params : Map.of();
        Map<String, MetricParamSchema> schema = metric.getParams().stream()
                .collect(Collectors.toMap(MetricParamSchema::key, p -> p, (a, b) -> a, LinkedHashMap::new));

        int errorsBefore = errors.size();
        source.keySet().stream()
                .filter(key -> !schema.containsKey(key))
                .forEach(key -> errors.add("params: неизвестный параметр '" + key + "' для метрики '" + metric.getTitle() + "'"));
        schema.values().stream()
                .filter(MetricParamSchema::required)
                .filter(p -> source.get(p.key()) == null || (source.get(p.key()) instanceof String s && s.isBlank()))
                .forEach(p -> errors.add("params: '" + p.key() + "' (" + p.label() + ") обязателен"));
        if (errors.size() > errorsBefore) {
            return Map.of();
        }

        try {
            return calculatorRegistry.get(metric.getCalculator()).normalizeParams(source);
        } catch (InvalidParamsException e) {
            e.getErrors().forEach(error -> errors.add("params: " + error));
            return Map.of();
        }
    }

    /**
     * Checks and normalizes the conditions of a stop factor: REJECTED or UNDERWRITER, at most one of each, a value
     * type and an operator that fit the metric, valueTo exactly for RANGE and BETWEEN, and an UNDERWRITER condition
     * that can fire where REJECTED does not. An operator left out is IN for loan statuses and GT for numbers, and an
     * UNDERWRITER number left without operator next to a REJECTED GT becomes RANGE up to the REJECTED value, as
     * legacy thresholds meant ({@link #withDefaultOperators}).
     */
    List<ConditionDto> validateConditions(StopFactorMetric metric, List<ConditionDto> conditions, List<String> errors) {
        ValueKind kind = calculatorRegistry.get(metric.getCalculator()).valueKind();
        List<ConditionDto> normalized = new ArrayList<>();
        Set<StopFactorStatus> seenResults = EnumSet.noneOf(StopFactorStatus.class);

        for (int i = 0; i < conditions.size(); i++) {
            ConditionDto condition = conditions.get(i);
            String prefix = "conditions[" + i + "]: ";
            if (!CONDITION_RESULTS.contains(condition.result())) {
                errors.add(prefix + "result должен быть REJECTED или UNDERWRITER");
                continue;
            }
            if (!seenResults.add(condition.result())) {
                errors.add(prefix + "допускается только одно условие " + condition.result());
                continue;
            }
            if (!kind.conditionTypes().contains(condition.valueType())) {
                errors.add(prefix + "тип значения " + condition.valueType() + " не подходит для метрики '" + metric.getTitle()
                        + "', допустимые: " + new TreeSet<>(kind.conditionTypes()));
                continue;
            }
            if (condition.operator() != null && !kind.operators().contains(condition.operator())) {
                errors.add(prefix + "оператор " + condition.operator() + " (" + condition.operator().getName()
                        + ") не подходит для метрики '" + metric.getTitle() + "', допустимые: " + kind.operators());
                continue;
            }
            String value = normalizeValue(condition.valueType(), condition.value(), prefix, errors);
            if (value == null) {
                continue;
            }
            ConditionOperator operator = condition.operator() != null ? condition.operator() : kind.operators().getFirst();
            String valueTo = normalizeValueTo(condition, operator, value, prefix, errors);
            if (operator.hasValueTo() && valueTo == null) {
                continue;
            }
            normalized.add(new ConditionDto(null, condition.result(), condition.valueType(), value,
                    condition.operator() != null ? operator : null, valueTo));
        }

        if (normalized.size() != conditions.size()) {
            return normalized;
        }
        List<ConditionDto> withOperators = withDefaultOperators(kind, normalized);
        checkConditionsConsistent(kind, withOperators, errors);
        return withOperators;
    }

    /**
     * valueTo of RANGE and BETWEEN: required, of the value type, above value. Other operators have none; errors are
     * added and null is returned when it does not fit.
     */
    private String normalizeValueTo(ConditionDto condition, ConditionOperator operator, String value, String prefix,
                                    List<String> errors) {
        boolean sent = condition.valueTo() != null && !condition.valueTo().isBlank();
        if (!operator.hasValueTo()) {
            if (sent) {
                errors.add(prefix + "valueTo указывается только для RANGE и BETWEEN, не для " + operator);
            }
            return null;
        }
        if (!sent) {
            errors.add(prefix + "valueTo обязателен для " + operator + " (" + operator.getName() + ")");
            return null;
        }
        String valueTo = normalizeValue(condition.valueType(), condition.valueTo(), prefix + "valueTo: ", errors);
        if (valueTo != null && ConditionValues.parseNumber(valueTo).compareTo(ConditionValues.parseNumber(value)) <= 0) {
            errors.add(prefix + "valueTo (" + valueTo + ") должно быть больше value (" + value + ")");
            return null;
        }
        return valueTo;
    }

    /**
     * Sets the operator of the conditions sent without one: IN for loan statuses, GT for numbers, and RANGE up to
     * the REJECTED value for an UNDERWRITER number next to a REJECTED GT number, so "1 - Ок, 2 - жёлтая зона,
     * 3+ - отказ" sent as legacy thresholds (UNDERWRITER 1, REJECTED 2) is stored as UNDERWRITER RANGE 1..2,
     * REJECTED GT 2.
     */
    static List<ConditionDto> withDefaultOperators(ValueKind kind, List<ConditionDto> conditions) {
        List<ConditionDto> withRejected = conditions.stream()
                .map(c -> c.result() == StopFactorStatus.REJECTED ? withDefaultOperator(kind, c) : c)
                .toList();
        Optional<String> rejectedGreaterThan = withRejected.stream()
                .filter(c -> c.result() == StopFactorStatus.REJECTED && c.operator() == ConditionOperator.GT)
                .map(ConditionDto::value)
                .findFirst();
        return withRejected.stream().map(c -> {
            if (c.operator() != null) {
                return c;
            }
            boolean belowRejected = kind == ValueKind.NUMBER && rejectedGreaterThan.isPresent()
                    && ConditionValues.parseNumber(c.value()).compareTo(ConditionValues.parseNumber(rejectedGreaterThan.get())) < 0;
            return belowRejected
                    ? new ConditionDto(c.id(), c.result(), c.valueType(), c.value(), ConditionOperator.RANGE, rejectedGreaterThan.get())
                    : withDefaultOperator(kind, c);
        }).toList();
    }

    private static ConditionDto withDefaultOperator(ValueKind kind, ConditionDto c) {
        return c.operator() != null ? c
                : new ConditionDto(c.id(), c.result(), c.valueType(), c.value(), kind.operators().getFirst(), c.valueTo());
    }

    void validateProducts(Set<Long> productIds, List<String> errors) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        Set<Long> found = stopFactorRepository.findProductsByIds(productIds).stream()
                .map(ProductProjection::getId)
                .collect(Collectors.toSet());
        productIds.stream()
                .filter(id -> !found.contains(id))
                .sorted()
                .forEach(id -> errors.add("productIds: продукт " + id + " не существует"));
    }

    private String normalizeValue(StopFactorValueTypes type, String raw, String prefix, List<String> errors) {
        String value = raw.trim();
        switch (type) {
            case INTEGER -> {
                try {
                    BigDecimal number = ConditionValues.parseNumber(value);
                    if (number.signum() < 0) {
                        errors.add(prefix + "значение не должно быть отрицательным");
                        return null;
                    }
                    return number.toBigIntegerExact().toString();
                } catch (NumberFormatException | ArithmeticException e) {
                    errors.add(prefix + "значение должно быть целым числом");
                    return null;
                }
            }
            case DOUBLE -> {
                try {
                    BigDecimal number = ConditionValues.parseNumber(value);
                    if (number.signum() < 0) {
                        errors.add(prefix + "значение не должно быть отрицательным");
                        return null;
                    }
                    return number.stripTrailingZeros().toPlainString();
                } catch (NumberFormatException e) {
                    errors.add(prefix + "значение должно быть числом");
                    return null;
                }
            }
            case ARRAY, STRING -> {
                List<String> items = Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                if (items.isEmpty()) {
                    errors.add(prefix + "значение должно содержать хотя бы один элемент");
                    return null;
                }
                if (type == StopFactorValueTypes.STRING && items.size() > 1) {
                    errors.add(prefix + "значение STRING должно быть одним элементом, для списка используйте ARRAY");
                    return null;
                }
                Set<String> unique = new HashSet<>();
                for (String item : items) {
                    if (!unique.add(ConditionValues.normalizeItem(item))) {
                        errors.add(prefix + "элемент '" + item + "' повторяется");
                        return null;
                    }
                }
                return String.join(", ", items);
            }
        }
        throw new IllegalArgumentException("Неподдерживаемый тип значения " + type);
    }

    /** The UNDERWRITER condition must be able to fire: some value must fire it and not REJECTED. */
    private void checkConditionsConsistent(ValueKind kind, List<ConditionDto> conditions, List<String> errors) {
        Optional<ConditionDto> rejected = conditions.stream().filter(c -> c.result() == StopFactorStatus.REJECTED).findFirst();
        Optional<ConditionDto> underwriter = conditions.stream().filter(c -> c.result() == StopFactorStatus.UNDERWRITER).findFirst();
        if (rejected.isEmpty() || underwriter.isEmpty()) {
            return;
        }
        if (kind == ValueKind.NUMBER) {
            if (NumberSet.of(underwriter.get()).within(NumberSet.of(rejected.get()))) {
                errors.add("conditions: UNDERWRITER (" + describe(underwriter.get()) + ") никогда не сработает: все его значения"
                        + " уже дают REJECTED (" + describe(rejected.get()) + "), REJECTED проверяется первым");
            }
        } else {
            Set<String> overlap = new HashSet<>(ConditionValues.parseItems(rejected.get().value()));
            overlap.retainAll(ConditionValues.parseItems(underwriter.get().value()));
            if (!overlap.isEmpty()) {
                errors.add("conditions: значения " + new TreeSet<>(overlap)
                        + " указаны и в REJECTED, и в UNDERWRITER, они всегда будут REJECTED");
            }
        }
    }

    private static String describe(ConditionDto c) {
        return c.operator().hasValueTo()
                ? c.operator().getName() + " " + c.value() + ".." + c.valueTo()
                : c.operator().getName() + " " + c.value();
    }

    /**
     * The numbers a number condition fires on, an interval: a null bound is unbounded.
     */
    private record NumberSet(BigDecimal from, boolean fromIncluded, BigDecimal to, boolean toIncluded) {

        static NumberSet of(ConditionDto c) {
            BigDecimal value = ConditionValues.parseNumber(c.value());
            BigDecimal valueTo = c.valueTo() != null ? ConditionValues.parseNumber(c.valueTo()) : null;
            return switch (c.operator()) {
                case GT -> new NumberSet(value, false, null, false);
                case GTE -> new NumberSet(value, true, null, false);
                case LT -> new NumberSet(null, false, value, false);
                case LTE -> new NumberSet(null, false, value, true);
                case EQ -> new NumberSet(value, true, value, true);
                case RANGE -> new NumberSet(value, false, valueTo, true);
                case BETWEEN -> new NumberSet(value, true, valueTo, true);
                case IN -> throw new IllegalArgumentException("IN is not a number condition");
            };
        }

        /** Every number of this set is in the other one. */
        boolean within(NumberSet other) {
            return startsWithin(other) && endsWithin(other);
        }

        private boolean startsWithin(NumberSet other) {
            if (other.from == null) {
                return true;
            }
            if (from == null) {
                return false;
            }
            int compare = from.compareTo(other.from);
            return compare > 0 || compare == 0 && (other.fromIncluded || !fromIncluded);
        }

        private boolean endsWithin(NumberSet other) {
            if (other.to == null) {
                return true;
            }
            if (to == null) {
                return false;
            }
            int compare = to.compareTo(other.to);
            return compare < 0 || compare == 0 && (other.toIncluded || !toIncluded);
        }
    }
}
