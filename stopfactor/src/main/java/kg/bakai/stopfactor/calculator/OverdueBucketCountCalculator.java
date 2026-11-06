package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.OverdueEpisode;
import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.enums.CalculatorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Count of continuous overdue episodes in a period of months whose maximum overdue is in a range of days
 * (both bounds included). Without episodes, or when they do not cover the whole period: the legacy counter with
 * exactly these params, if there is one.
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
                request.history() != null ? request.history().overdueEpisodesUpTo(bucket.periodTo()) : null,
                bucket::contains,
                () -> request.values().legacy().overdueEpisodes()
                        .count(bucket.periodFrom(), bucket.periodTo(), bucket.minOverdue(), bucket.maxOverdue()));
    }
}
