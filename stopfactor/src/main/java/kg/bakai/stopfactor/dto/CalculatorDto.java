package kg.bakai.stopfactor.dto;

import kg.bakai.stopfactor.enums.ValueKind;
import kg.bakai.stopfactor.enums.CalculatorType;
import kg.bakai.stopfactor.enums.ConditionOperator;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;

import java.util.List;
import java.util.Set;

public record CalculatorDto(
        CalculatorType type,
        String name,
        ValueKind valueKind,
        Set<StopFactorValueTypes> allowedConditionValueTypes,
        List<OperatorOption> allowedOperators,
        List<CalculatorParam> params
) {
    /**
     * An option of the console operator select: code is sent in ConditionDto.operator, name is shown.
     *
     * @param code GT, RANGE, IN
     * @param name ">", "> и <=", "in"
     */
    public record OperatorOption(ConditionOperator code, String name) {

        public static OperatorOption of(ConditionOperator operator) {
            return new OperatorOption(operator, operator.getName());
        }
    }

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
