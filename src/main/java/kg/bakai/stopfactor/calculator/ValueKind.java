package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.enums.StopFactorValueTypes;

import java.util.Set;

public enum ValueKind {
    NUMBER(Set.of(StopFactorValueTypes.INTEGER, StopFactorValueTypes.DOUBLE)),
    LIST(Set.of(StopFactorValueTypes.ARRAY, StopFactorValueTypes.STRING));

    private final Set<StopFactorValueTypes> conditionTypes;

    ValueKind(Set<StopFactorValueTypes> conditionTypes) {
        this.conditionTypes = conditionTypes;
    }

    public Set<StopFactorValueTypes> conditionTypes() {
        return conditionTypes;
    }
}
