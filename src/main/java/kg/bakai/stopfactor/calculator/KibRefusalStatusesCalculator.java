package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.enums.CalculatorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Override
    public Optional<CheckedValue> calculate(Map<String, Object> params, StopFactorCheckRequest request) {
        List<String> statuses = request.creditHistoryStatuses() != null
                ? request.creditHistoryStatuses().stream().filter(Objects::nonNull).toList()
                : List.of();
        return Optional.of(CheckedValue.ofValues(statuses));
    }
}
