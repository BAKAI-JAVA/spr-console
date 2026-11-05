package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.enums.CalculatorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class KibRefusalStatusesCalculator extends ParameterlessCalculator {

    @Override
    public CalculatorType type() {
        return CalculatorType.KIB_REFUSAL_STATUSES;
    }

    @Override
    public ValueKind valueKind() {
        return ValueKind.LIST;
    }

    /** Statuses of the KIB loans of the client; the condition of the stop factor lists the refusal ones. */
    @Override
    public Optional<CheckedValue> calculate(NoParams params, StopFactorCheckRequest request) {
        return Optional.of(CheckedValue.ofValues(request.loanStatuses() != null ? request.loanStatuses() : List.of()));
    }
}
