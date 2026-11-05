package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.ReportValues;
import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.enums.CalculatorType;

import java.util.Optional;
import java.util.function.ToDoubleFunction;

/** A value of the reports as is, without params: disputes, current overdue, open credits. */
public class SimpleValueCalculator extends ParameterlessCalculator {

    private final CalculatorType type;
    private final ToDoubleFunction<ReportValues> extractor;

    public SimpleValueCalculator(CalculatorType type, ToDoubleFunction<ReportValues> extractor) {
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
    public Optional<CheckedValue> calculate(NoParams params, StopFactorCheckRequest request) {
        return Optional.of(CheckedValue.ofNumber(extractor.applyAsDouble(request.values())));
    }
}
