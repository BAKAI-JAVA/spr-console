package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.enums.ConditionOperator;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;

import java.util.List;
import java.util.Set;

public enum ValueKind {
    NUMBER(Set.of(StopFactorValueTypes.INTEGER, StopFactorValueTypes.DOUBLE), List.of(
            ConditionOperator.GT, ConditionOperator.GTE, ConditionOperator.LT, ConditionOperator.LTE, ConditionOperator.EQ,
            ConditionOperator.RANGE, ConditionOperator.BETWEEN)),
    LIST(Set.of(StopFactorValueTypes.ARRAY, StopFactorValueTypes.STRING), List.of(ConditionOperator.IN));

    private final Set<StopFactorValueTypes> conditionTypes;
    private final List<ConditionOperator> operators;

    ValueKind(Set<StopFactorValueTypes> conditionTypes, List<ConditionOperator> operators) {
        this.conditionTypes = conditionTypes;
        this.operators = operators;
    }

    public Set<StopFactorValueTypes> conditionTypes() {
        return conditionTypes;
    }

    /** Condition operators of this kind of value. */
    public List<ConditionOperator> operators() {
        return operators;
    }
}
