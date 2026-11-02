package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.dto.scoring.StopFactorIntegerValueDto;
import kg.bakai.stopfactor.enums.CalculatorType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

public class SimpleValueCalculator extends AbstractStopFactorCalculator {

    private final CalculatorType type;
    private final ToDoubleFunction<StopFactorIntegerValueDto> extractor;

    public SimpleValueCalculator(CalculatorType type, ToDoubleFunction<StopFactorIntegerValueDto> extractor) {
        this.type = type;
        this.extractor = extractor;
    }

    @Override
    public CalculatorType type() {
        return type;
    }

    @Override
    public ValueKind valueKind() {
        return ValueKind.NUMBER;
    }

    @Override
    public List<ParamSpec> paramSpecs() {
        return List.of();
    }

    @Override
    public Optional<CheckedValue> calculate(Map<String, Object> params, StopFactorCheckRequest request) {
        return Optional.of(CheckedValue.ofNumber(extractor.applyAsDouble(request.values())));
    }
}
