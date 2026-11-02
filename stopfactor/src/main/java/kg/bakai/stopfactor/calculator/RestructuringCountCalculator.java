package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents;
import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.dto.scoring.StopFactorIntegerValueDto;
import kg.bakai.stopfactor.enums.CalculatorType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.ToDoubleFunction;

@Component
public class RestructuringCountCalculator extends AbstractStopFactorCalculator {

    static final String SOURCE = "source";
    static final String PERIOD_FROM = "periodFromMonth";
    static final String PERIOD_TO = "periodToMonth";

    private record Window(CreditHistoryEvents.Source source, int periodFrom, int periodTo) {
    }

    private static final Map<Window, ToDoubleFunction<StopFactorIntegerValueDto>> LEGACY_FIELDS = Map.of(
            new Window(CreditHistoryEvents.Source.KIB, 1, 12), StopFactorIntegerValueDto::numberOfRestructuringsLast12Months,
            new Window(CreditHistoryEvents.Source.OB, 1, 12), StopFactorIntegerValueDto::countOfChangesObLast12Month,
            new Window(CreditHistoryEvents.Source.KIB, 13, 24), StopFactorIntegerValueDto::numberOfRestructuringsFrom12To24Months,
            new Window(CreditHistoryEvents.Source.OB, 13, 24), StopFactorIntegerValueDto::countOfChangesObFrom13To24Months
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
    public Optional<CheckedValue> calculate(Map<String, Object> params, StopFactorCheckRequest request) {
        Window window = new Window(
                CreditHistoryEvents.Source.valueOf(stringParam(params, SOURCE)),
                intParam(params, PERIOD_FROM),
                intParam(params, PERIOD_TO)
        );

        List<CreditHistoryEvents.RestructuringEvent> events = request.history() != null ? request.history().restructurings() : null;
        if (events != null) {
            long count = events.stream()
                    .filter(e -> e.source() == window.source())
                    .filter(e -> e.monthsAgo() >= window.periodFrom() && e.monthsAgo() <= window.periodTo())
                    .count();
            return Optional.of(CheckedValue.ofNumber(count));
        }

        ToDoubleFunction<StopFactorIntegerValueDto> legacyField = LEGACY_FIELDS.get(window);
        if (legacyField == null) {
            return Optional.empty();
        }
        return Optional.of(CheckedValue.ofNumber(legacyField.applyAsDouble(request.values())));
    }
}
