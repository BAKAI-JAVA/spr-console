package kg.bakai.stopfactor.dto.scoring;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.ToIntFunction;

/**
 * Overdue episode counters of the old algorithm (SPR KibService.getOverdueByPeriodAndCount). Each one is named by
 * the stop factor that reads it: months of the period and maximum overdue days, both bounds included, "From" - no
 * upper bound. E.g. {@code months7To12Days5To14} is read by the stop factor
 * {@code "с 7 по 12 месяц, максимальная просрочка >= 5 и <= 14"}.
 * <p>
 * SPR names some of them by other ranges ({@link #from} keeps the original pairing, so the original stop factors
 * keep their results): its "1-15 days" is read as 1-10, "16-30" as 11-15, "31+" as 16+ in months 1..6, and so on.
 */
public record LegacyOverdueCounters(
        int months1To3Days1To10,
        int months1To3Days11To15,
        int months1To3DaysFrom16,
        int months4To6Days1To10,
        int months4To6Days11To15,
        int months4To6DaysFrom16,
        int months7To12Days1To4,
        int months7To12Days5To14,
        int months7To12Days15To20,
        int months7To12DaysFrom21,
        int months13To24Days1To7,
        int months13To24Days8To20,
        int months13To24Days21To30,
        int months13To24DaysFrom31,
        int months25To36Days1To29,
        int months25To36Days30To90,
        int months25To36DaysFrom91
) {

    /** No overdue in any period. */
    public static final LegacyOverdueCounters NONE = new LegacyOverdueCounters(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    /** Period in months and maximum overdue in days of a counter; maxDays null - no upper bound. */
    private record Bucket(int periodFromMonth, int periodToMonth, int minDays, Integer maxDays) {
    }

    private static final Map<Bucket, ToIntFunction<LegacyOverdueCounters>> COUNTERS = Map.ofEntries(
            Map.entry(new Bucket(1, 3, 1, 10), LegacyOverdueCounters::months1To3Days1To10),
            Map.entry(new Bucket(1, 3, 11, 15), LegacyOverdueCounters::months1To3Days11To15),
            Map.entry(new Bucket(1, 3, 16, null), LegacyOverdueCounters::months1To3DaysFrom16),
            Map.entry(new Bucket(4, 6, 1, 10), LegacyOverdueCounters::months4To6Days1To10),
            Map.entry(new Bucket(4, 6, 11, 15), LegacyOverdueCounters::months4To6Days11To15),
            Map.entry(new Bucket(4, 6, 16, null), LegacyOverdueCounters::months4To6DaysFrom16),
            Map.entry(new Bucket(7, 12, 1, 4), LegacyOverdueCounters::months7To12Days1To4),
            Map.entry(new Bucket(7, 12, 5, 14), LegacyOverdueCounters::months7To12Days5To14),
            Map.entry(new Bucket(7, 12, 15, 20), LegacyOverdueCounters::months7To12Days15To20),
            Map.entry(new Bucket(7, 12, 21, null), LegacyOverdueCounters::months7To12DaysFrom21),
            Map.entry(new Bucket(13, 24, 1, 7), LegacyOverdueCounters::months13To24Days1To7),
            Map.entry(new Bucket(13, 24, 8, 20), LegacyOverdueCounters::months13To24Days8To20),
            Map.entry(new Bucket(13, 24, 21, 30), LegacyOverdueCounters::months13To24Days21To30),
            Map.entry(new Bucket(13, 24, 31, null), LegacyOverdueCounters::months13To24DaysFrom31),
            Map.entry(new Bucket(25, 36, 1, 29), LegacyOverdueCounters::months25To36Days1To29),
            Map.entry(new Bucket(25, 36, 30, 90), LegacyOverdueCounters::months25To36Days30To90),
            Map.entry(new Bucket(25, 36, 91, null), LegacyOverdueCounters::months25To36DaysFrom91)
    );

    /** The SPR counters under the names of the stop factors that read them; null (no counters) is NONE. */
    public static LegacyOverdueCounters from(OverdueForStopFactor spr) {
        if (spr == null) {
            return NONE;
        }
        return new LegacyOverdueCounters(
                spr.countWherePeriodFrom1To3MonthsAndOverdueFrom1To15(),
                spr.countWherePeriodFrom1To3MonthsAndOverdueFrom16To30(),
                spr.countWherePeriodFrom1To3MonthsAndOverdueFrom31(),
                spr.countWherePeriodFrom4To6MonthsAndOverdueFrom1To15(),
                spr.countWherePeriodFrom4To6MonthsAndOverdueFrom16To30(),
                spr.countWherePeriodFrom4To6MonthsAndOverdueFrom31(),
                spr.countWherePeriodFrom7To12MonthsAndOverdueFrom1To10(),
                spr.countWherePeriodFrom7To12MonthsAndOverdueFrom11To20(),
                spr.countWherePeriodFrom7To12MonthsAndOverdueFrom21To30(),
                spr.countWherePeriodFrom7To12MonthsAndOverdueFrom31(),
                spr.countWherePeriodFrom13To24MonthsAndOverdueFrom1To10(),
                spr.countWherePeriodFrom13To24MonthsAndOverdueFrom11To20(),
                spr.countWherePeriodFrom13To24MonthsAndOverdueFrom21To30(),
                spr.countWherePeriodFrom13To24MonthsAndOverdueFrom31(),
                spr.countWherePeriodFrom25To36MonthsAndOverdueFrom1To60(),
                spr.countWherePeriodFrom25To36MonthsAndOverdueFrom61To90(),
                spr.countWherePeriodFrom25To36MonthsAndOverdueFrom91());
    }

    /**
     * Episodes in months periodFrom..periodTo with a maximum overdue of minDays..maxDays; empty when no counter
     * has exactly these params.
     */
    public OptionalDouble count(int periodFromMonth, int periodToMonth, int minDays, Integer maxDays) {
        ToIntFunction<LegacyOverdueCounters> counter = COUNTERS.get(new Bucket(periodFromMonth, periodToMonth, minDays, maxDays));
        return counter != null ? OptionalDouble.of(counter.applyAsInt(this)) : OptionalDouble.empty();
    }
}
