package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents;
import kg.bakai.stopfactor.dto.scoring.LegacyCounters;
import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.enums.CalculatorType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Count of events (bureau requests, new credits) in the last N days. Without the raw event dates: the legacy
 * counter, which has one period only (7 days of bureau requests, 30 days of new credits).
 */
public class RecentEventsCountCalculator extends AbstractStopFactorCalculator<RecentEventsCountCalculator.Period> {

    static final String DAYS = "days";

    public record Period(int days) {
    }

    private final CalculatorType type;
    private final Function<CreditHistoryEvents, List<Integer>> rawEventsDaysAgo;
    private final BiFunction<LegacyCounters, Integer, OptionalDouble> legacyCount;

    /**
     * @param rawEventsDaysAgo the raw events of the history: days between each event and the request
     * @param legacyCount      the legacy counter for a number of days, empty for other periods
     */
    public RecentEventsCountCalculator(CalculatorType type,
                                       Function<CreditHistoryEvents, List<Integer>> rawEventsDaysAgo,
                                       BiFunction<LegacyCounters, Integer, OptionalDouble> legacyCount) {
        this.type = type;
        this.rawEventsDaysAgo = rawEventsDaysAgo;
        this.legacyCount = legacyCount;
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
                request.history() != null ? rawEventsDaysAgo.apply(request.history()) : null,
                daysAgo -> daysAgo <= period.days(),
                () -> legacyCount.apply(request.values().legacy(), period.days()));
    }
}
