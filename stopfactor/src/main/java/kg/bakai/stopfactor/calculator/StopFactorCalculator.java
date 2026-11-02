package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.enums.CalculatorType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StopFactorCalculator {

    CalculatorType type();

    ValueKind valueKind();

    List<ParamSpec> paramSpecs();

    Map<String, Object> normalizeParams(Map<String, Object> params);

    Optional<CheckedValue> calculate(Map<String, Object> params, StopFactorCheckRequest request);
}
