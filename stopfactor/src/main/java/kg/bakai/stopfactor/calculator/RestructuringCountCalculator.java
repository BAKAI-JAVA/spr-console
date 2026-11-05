package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents;
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.RestructuringEvent;
import kg.bakai.stopfactor.dto.scoring.ReportValues;
import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.enums.CalculatorType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/** Count of restructurings in KIB, or changes of credits in OB, in a period of months. */
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

    // The only windows the reports have a value for; SPR does not provide raw restructuring events
    private static final Map<Window, ToDoubleFunction<ReportValues>> LEGACY_FIELDS = Map.of(
            new Window(CreditHistoryEvents.Source.KIB, 1, 12), ReportValues::numberOfRestructuringsLast12Months,
            new Window(CreditHistoryEvents.Source.OB, 1, 12), ReportValues::countOfChangesObLast12Month,
            new Window(CreditHistoryEvents.Source.KIB, 13, 24), ReportValues::numberOfRestructuringsFrom12To24Months,
            new Window(CreditHistoryEvents.Source.OB, 13, 24), ReportValues::countOfChangesObFrom13To24Months
    );

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
                () -> {
                    ToDoubleFunction<ReportValues> legacyField = LEGACY_FIELDS.get(window);
                    return legacyField != null ? OptionalDouble.of(legacyField.applyAsDouble(request.values())) : OptionalDouble.empty();
                });
    }
}
