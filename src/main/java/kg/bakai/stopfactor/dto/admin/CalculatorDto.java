package kg.bakai.stopfactor.dto.admin;

import kg.bakai.stopfactor.calculator.ValueKind;
import kg.bakai.stopfactor.enums.CalculatorType;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;

import java.util.List;
import java.util.Set;

public record CalculatorDto(
        CalculatorType type,
        String name,
        ValueKind valueKind,
        Set<StopFactorValueTypes> allowedConditionValueTypes,
        List<CalculatorParam> params
) {
    public record CalculatorParam(
            String key,
            String label,
            boolean required,
            String type,
            Integer min,
            Set<String> allowedValues
    ) {
    }
}
