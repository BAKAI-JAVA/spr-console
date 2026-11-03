package kg.bakai.stopfactor.service.console.impl;

import kg.bakai.stopfactor.calculator.CalculatorRegistry;
import kg.bakai.stopfactor.calculator.InvalidParamsException;
import kg.bakai.stopfactor.calculator.StopFactorCalculator;
import kg.bakai.stopfactor.calculator.ValueKind;
import kg.bakai.stopfactor.dto.console.MetricParamSchema;
import kg.bakai.stopfactor.dto.console.ConditionDto;
import kg.bakai.stopfactor.entity.StopFactorMetric;
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

    List<ConditionDto> validateConditions(StopFactorMetric metric, List<ConditionDto> conditions, List<String> errors) {
        StopFactorCalculator calculator = calculatorRegistry.get(metric.getCalculator());
        ValueKind kind = calculator.valueKind();
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
            String value = normalizeValue(condition.valueType(), condition.value(), prefix, errors);
            if (value != null) {
                normalized.add(new ConditionDto(null, condition.result(), condition.valueType(), value));
            }
        }

        if (normalized.size() == conditions.size()) {
            checkConditionsConsistent(kind, normalized, errors);
        }
        return normalized;
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

    private void checkConditionsConsistent(ValueKind kind, List<ConditionDto> conditions, List<String> errors) {
        Optional<ConditionDto> rejected = conditions.stream().filter(c -> c.result() == StopFactorStatus.REJECTED).findFirst();
        Optional<ConditionDto> underwriter = conditions.stream().filter(c -> c.result() == StopFactorStatus.UNDERWRITER).findFirst();
        if (rejected.isEmpty() || underwriter.isEmpty()) {
            return;
        }
        if (kind == ValueKind.NUMBER) {
            BigDecimal rejectedValue = ConditionValues.parseNumber(rejected.get().value());
            BigDecimal underwriterValue = ConditionValues.parseNumber(underwriter.get().value());
            if (underwriterValue.compareTo(rejectedValue) >= 0) {
                errors.add("conditions: значение UNDERWRITER (" + underwriterValue + ") должно быть меньше значения REJECTED ("
                        + rejectedValue + "), иначе UNDERWRITER никогда не сработает");
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
}
