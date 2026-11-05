package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.OverdueEpisode;
import kg.bakai.stopfactor.dto.scoring.OverdueForStopFactor;
import kg.bakai.stopfactor.dto.scoring.ReportValues;
import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.enums.CalculatorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.ToIntFunction;

/**
 * Count of continuous overdue episodes in a period of months whose maximum overdue is in a range of days
 * (both bounds included).
 */
@Component
public class OverdueBucketCountCalculator extends AbstractStopFactorCalculator<OverdueBucketCountCalculator.Bucket> {

    static final String PERIOD_FROM = "periodFromMonth";
    static final String PERIOD_TO = "periodToMonth";
    static final String MIN_OVERDUE = "minOverdueDays";
    static final String MAX_OVERDUE = "maxOverdueDays";

    /**
     * @param maxOverdue null - no upper bound
     */
    public record Bucket(int periodFrom, int periodTo, int minOverdue, Integer maxOverdue) {

        boolean contains(OverdueEpisode episode) {
            return episode.monthsAgo() >= periodFrom && episode.monthsAgo() <= periodTo
                    && episode.maxOverdueDays() >= minOverdue
                    && (maxOverdue == null || episode.maxOverdueDays() <= maxOverdue);
        }
    }
    // Legacy counter read by each original stop factor's params. The field names do not match the ranges:
    // this mapping keeps the results the original stop factors always had
    private static final Map<Bucket, ToIntFunction<OverdueForStopFactor>> LEGACY_FIELDS = Map.ofEntries(
            Map.entry(new Bucket(1, 3, 1, 10), OverdueForStopFactor::countWherePeriodFrom1To3MonthsAndOverdueFrom1To15),
            Map.entry(new Bucket(1, 3, 11, 15), OverdueForStopFactor::countWherePeriodFrom1To3MonthsAndOverdueFrom16To30),
            Map.entry(new Bucket(1, 3, 16, null), OverdueForStopFactor::countWherePeriodFrom1To3MonthsAndOverdueFrom31),
            Map.entry(new Bucket(4, 6, 1, 10), OverdueForStopFactor::countWherePeriodFrom4To6MonthsAndOverdueFrom1To15),
            Map.entry(new Bucket(4, 6, 11, 15), OverdueForStopFactor::countWherePeriodFrom4To6MonthsAndOverdueFrom16To30),
            Map.entry(new Bucket(4, 6, 16, null), OverdueForStopFactor::countWherePeriodFrom4To6MonthsAndOverdueFrom31),
            Map.entry(new Bucket(7, 12, 1, 4), OverdueForStopFactor::countWherePeriodFrom7To12MonthsAndOverdueFrom1To10),
            Map.entry(new Bucket(7, 12, 5, 14), OverdueForStopFactor::countWherePeriodFrom7To12MonthsAndOverdueFrom11To20),
            Map.entry(new Bucket(7, 12, 15, 20), OverdueForStopFactor::countWherePeriodFrom7To12MonthsAndOverdueFrom21To30),
            Map.entry(new Bucket(7, 12, 21, null), OverdueForStopFactor::countWherePeriodFrom7To12MonthsAndOverdueFrom31),
            Map.entry(new Bucket(13, 24, 1, 7), OverdueForStopFactor::countWherePeriodFrom13To24MonthsAndOverdueFrom1To10),
            Map.entry(new Bucket(13, 24, 8, 20), OverdueForStopFactor::countWherePeriodFrom13To24MonthsAndOverdueFrom11To20),
            Map.entry(new Bucket(13, 24, 21, 30), OverdueForStopFactor::countWherePeriodFrom13To24MonthsAndOverdueFrom21To30),
            Map.entry(new Bucket(13, 24, 31, null), OverdueForStopFactor::countWherePeriodFrom13To24MonthsAndOverdueFrom31),
            Map.entry(new Bucket(25, 36, 1, 29), OverdueForStopFactor::countWherePeriodFrom25To36MonthsAndOverdueFrom1To60),
            Map.entry(new Bucket(25, 36, 30, 90), OverdueForStopFactor::countWherePeriodFrom25To36MonthsAndOverdueFrom61To90),
            Map.entry(new Bucket(25, 36, 91, null), OverdueForStopFactor::countWherePeriodFrom25To36MonthsAndOverdueFrom91)
    );

    @Override
    public CalculatorType type() {
        return CalculatorType.OVERDUE_BUCKET_COUNT;
    }

    @Override
    public ValueKind valueKind() {
        return ValueKind.NUMBER;
    }

    @Override
    public List<ParamSpec> paramSpecs() {
        return List.of(
                ParamSpec.integer(PERIOD_FROM, "Период с (месяц)", true, 1),
                ParamSpec.integer(PERIOD_TO, "Период по (месяц)", true, 1),
                ParamSpec.integer(MIN_OVERDUE, "Максимальная просрочка от (дней)", true, 1),
                ParamSpec.integer(MAX_OVERDUE, "Максимальная просрочка до (дней)", false, 1)
        );
    }

    @Override
    protected void validateNormalized(Map<String, Object> params, List<String> errors) {
        requireOrdered(params, PERIOD_FROM, PERIOD_TO, errors);
        requireOrdered(params, MIN_OVERDUE, MAX_OVERDUE, errors);
    }

    @Override
    public Bucket parseParams(Map<String, Object> params) {
        return new Bucket(
                intParam(params, PERIOD_FROM),
                intParam(params, PERIOD_TO),
                intParam(params, MIN_OVERDUE),
                optionalIntParam(params, MAX_OVERDUE));
    }

    @Override
    public Optional<CheckedValue> calculate(Bucket bucket, StopFactorCheckRequest request) {
        return EventCount.count(
                request.history() != null ? request.history().overdueEpisodes() : null,
                bucket::contains,
                () -> legacyCount(bucket, request.values()));
    }

    private static OptionalDouble legacyCount(Bucket bucket, ReportValues values) {
        ToIntFunction<OverdueForStopFactor> legacyField = LEGACY_FIELDS.get(bucket);
        if (legacyField == null) {
            return OptionalDouble.empty();
        }
        OverdueForStopFactor overdue = values.overdue();
        return OptionalDouble.of(overdue != null ? legacyField.applyAsInt(overdue) : 0);
    }
}
