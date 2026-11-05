package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents;
import kg.bakai.stopfactor.dto.scoring.ReportValues;
import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.enums.CalculatorType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Count of events (bureau requests, new credits) in the last N days. The reports have a value for one period
 * only (legacyDays); other periods need the raw event dates.
 */
public class RecentEventsCountCalculator extends AbstractStopFactorCalculator<RecentEventsCountCalculator.Period> {

    static final String DAYS = "days";

    public record Period(int days) {
    }

    private final CalculatorType type;
    private final Function<CreditHistoryEvents, List<Integer>> rawEvents;
    private final int legacyDays;
    private final ToDoubleFunction<ReportValues> legacyField;

    public RecentEventsCountCalculator(CalculatorType type,
                                       Function<CreditHistoryEvents, List<Integer>> rawEvents,
                                       int legacyDays,
                                       ToDoubleFunction<ReportValues> legacyField) {
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
    public Period parseParams(Map<String, Object> params) {
        return new Period(intParam(params, DAYS));
    }

    @Override
    public Optional<CheckedValue> calculate(Period period, StopFactorCheckRequest request) {
        return EventCount.count(
                request.history() != null ? rawEvents.apply(request.history()) : null,
                daysAgo -> daysAgo <= period.days(),
                () -> period.days() == legacyDays ? OptionalDouble.of(legacyField.applyAsDouble(request.values())) : OptionalDouble.empty());
    }
}
