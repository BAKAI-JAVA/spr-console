package kg.bakai.stopfactor.evaluation;

import kg.bakai.stopfactor.calculator.CheckedValue;
import kg.bakai.stopfactor.entity.StopFactorCondition;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
public class ConditionMatcher {

    // Числовое условие хранит максимально допустимое значение и срабатывает, когда значение больше него.
    // Условие-список срабатывает, когда значение содержит один из элементов списка. REJECTED важнее UNDERWRITER.
    public StopFactorStatus evaluate(CheckedValue value, List<StopFactorCondition> conditions) {
        if (matchesAny(value, conditions, StopFactorStatus.REJECTED)) {
            return StopFactorStatus.REJECTED;
        }
        if (matchesAny(value, conditions, StopFactorStatus.UNDERWRITER)) {
            return StopFactorStatus.UNDERWRITER;
        }
        return StopFactorStatus.APPROVED;
    }

    private boolean matchesAny(CheckedValue value, List<StopFactorCondition> conditions, StopFactorStatus result) {
        return conditions.stream()
                .filter(c -> c.getResult() == result)
                .anyMatch(c -> matches(value, c));
    }

    private boolean matches(CheckedValue value, StopFactorCondition condition) {
        return switch (condition.getValueType()) {
            case INTEGER, DOUBLE -> {
                requireKind(value.isNumber(), condition);
                yield BigDecimal.valueOf(value.number()).compareTo(ConditionValues.parseNumber(condition.getValue())) > 0;
            }
            case ARRAY, STRING -> {
                requireKind(!value.isNumber(), condition);
                Set<String> conditionValues = ConditionValues.parseItems(condition.getValue());
                yield value.values().stream()
                        .map(ConditionValues::normalizeItem)
                        .anyMatch(conditionValues::contains);
            }
        };
    }

    private void requireKind(boolean matches, StopFactorCondition condition) {
        if (!matches) {
            throw new IllegalStateException("Условие " + condition.getId() + " с типом " + condition.getValueType()
                    + " не соответствует значению метрики стоп-фактора");
        }
    }
}
