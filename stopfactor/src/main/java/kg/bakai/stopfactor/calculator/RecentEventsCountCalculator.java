package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents;
import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.dto.scoring.StopFactorIntegerValueDto;
import kg.bakai.stopfactor.enums.CalculatorType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class RecentEventsCountCalculator extends AbstractStopFactorCalculator {

    static final String DAYS = "days";

    private final CalculatorType type;
    private final Function<CreditHistoryEvents, List<Integer>> rawEvents;
    private final int legacyDays;
    private final ToDoubleFunction<StopFactorIntegerValueDto> legacyField;

    public RecentEventsCountCalculator(CalculatorType type,
                                       Function<CreditHistoryEvents, List<Integer>> rawEvents,
                                       int legacyDays,
                                       ToDoubleFunction<StopFactorIntegerValueDto> legacyField) {
        this.type = type;
        this.rawEvents = rawEvents;
        this.legacyDays = legacyDays;
        this.legacyField = legacyField;
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
        return List.of(ParamSpec.integer(DAYS, "Последние N дней", true, 1));
    }

    @Override
    public Optional<CheckedValue> calculate(Map<String, Object> params, StopFactorCheckRequest request) {
        int days = intParam(params, DAYS);

        List<Integer> events = request.history() != null ? rawEvents.apply(request.history()) : null;
        if (events != null) {
            return Optional.of(CheckedValue.ofNumber(events.stream().filter(daysAgo -> daysAgo <= days).count()));
        }
        if (days != legacyDays) {
            return Optional.empty();
        }
        return Optional.of(CheckedValue.ofNumber(legacyField.applyAsDouble(request.values())));
    }
}
