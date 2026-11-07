package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.calculator.AbstractStopFactorCalculator;
import kg.bakai.stopfactor.calculator.CheckedValue;
import kg.bakai.stopfactor.calculator.ParamSpec;
import kg.bakai.stopfactor.enums.ValueKind;
import kg.bakai.stopfactor.enums.CalculatorType;
import kg.bakai.stopfactor.report.CreditHistoryEvents.RestructuringEvent;
import kg.bakai.stopfactor.report.CreditHistoryEvents;
import kg.bakai.stopfactor.report.StopFactorCheckRequest;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Count of restructurings in KIB, or changes of credits in OB, in a period of months. Without raw events: the legacy
 * counter of months 1..12 or 13..24.
 */
@Component
public class RestructuringCountCalculator extends AbstractStopFactorCalculator<RestructuringCountCalculator.Window> {

    static final String SOURCE = "source";
    static final String PERIOD_FROM = "periodFromMonth";
    static final String PERIOD_TO = "periodToMonth";

    public record Window(CreditHistoryEvents.Source source, int periodFrom, int periodTo) {

        boolean contains(RestructuringEvent event) {
            return event.source() == source && event.monthsAgo() >= periodFrom && event.monthsAgo() <= periodTo;
        }
    }

    @Override
    public CalculatorType type() {
        return CalculatorType.RESTRUCTURING_COUNT;
    }

    @Override
    public ValueKind valueKind() {
        return ValueKind.NUMBER;
    }

    @Override
    public List<ParamSpec> paramSpecs() {
        Set<String> sources = new LinkedHashSet<>();
        Arrays.stream(CreditHistoryEvents.Source.values()).forEach(s -> sources.add(s.name()));
        return List.of(
                ParamSpec.oneOf(SOURCE, "Источник (KIB или OB)", sources),
                ParamSpec.integer(PERIOD_FROM, "Период с (месяц)", true, 1),
                ParamSpec.integer(PERIOD_TO, "Период по (месяц)", true, 1)
        );
    }

    @Override
    protected void validateNormalized(Map<String, Object> params, List<String> errors) {
        requireOrdered(params, PERIOD_FROM, PERIOD_TO, errors);
    }

    @Override
    public Window parseParams(Map<String, Object> params) {
        return new Window(
                CreditHistoryEvents.Source.valueOf(stringParam(params, SOURCE)),
                intParam(params, PERIOD_FROM),
                intParam(params, PERIOD_TO));
    }

    @Override
    public Optional<CheckedValue> calculate(Window window, StopFactorCheckRequest request) {
        return EventCount.count(
                request.history() != null ? request.history().restructurings() : null,
                window::contains,
                () -> request.values().legacy().restructurings(window.source(), window.periodFrom(), window.periodTo()));
    }
}
