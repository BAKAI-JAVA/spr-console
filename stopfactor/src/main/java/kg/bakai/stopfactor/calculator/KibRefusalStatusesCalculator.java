package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.enums.CalculatorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KibRefusalStatusesCalculator extends AbstractStopFactorCalculator {

    @Override
    public CalculatorType type() {
        return CalculatorType.KIB_REFUSAL_STATUSES;
    }

    @Override
    public ValueKind valueKind() {
        return ValueKind.LIST;
    }

    @Override
    public List<ParamSpec> paramSpecs() {
        return List.of();
    }

    /**
     * The statuses are matched by the stop factor's condition against the loans of the KIB report, the
     * calculator itself has no value to measure.
     */
    @Override
    public Optional<CheckedValue> calculate(Map<String, Object> params, StopFactorCheckRequest request) {
        return Optional.of(CheckedValue.ofValues(List.of()));
    }
}
