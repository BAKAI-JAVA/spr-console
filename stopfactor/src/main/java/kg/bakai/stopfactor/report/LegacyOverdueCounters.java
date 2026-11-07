package kg.bakai.stopfactor.report;

import kg.bakai.spr.core.kib.dto.OverdueForStopFactor;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.ToIntFunction;

/**
 * Overdue counters of the KIB module (KibOverdueCalculator, KibService.getOverdueByPeriodAndCount), named by what
 * it counts: months of the period and overdue days, both bounds included, "From" - no upper bound.
 * <p>
 * The KIB module counts the payment calendar <b>months</b> with an overdue ({@code MaxPastDueDays} of 1 and more)
 * of the contracts where the client is the <b>main debtor</b>; month 1 is the month of the request.
 * KibOverdueHistoryReader counts the same months from the same calendars, so with the calendars there the
 * counters add nothing: they are the fallback of the stop factors whose params are exactly one counter.
 */
public record LegacyOverdueCounters(
        int months1To3Days1To15,
        int months1To3Days16To30,
        int months1To3DaysFrom31,
        int months4To6Days1To15,
        int months4To6Days16To30,
        int months4To6DaysFrom31,
        int months7To12Days1To10,
        int months7To12Days11To20,
        int months7To12Days21To30,
        int months7To12DaysFrom31,
        int months13To24Days1To10,
        int months13To24Days11To20,
        int months13To24Days21To30,
        int months13To24DaysFrom31,
        int months25To36Days1To60,
        int months25To36Days61To90,
        int months25To36DaysFrom91
) {

    /** No overdue in any period. */
    public static final LegacyOverdueCounters NONE = new LegacyOverdueCounters(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    /** Period in months and overdue in days of a counter; maxDays null - no upper bound. */
    private record Bucket(int periodFromMonth, int periodToMonth, int minDays, Integer maxDays) {
    }

    private static final Map<Bucket, ToIntFunction<LegacyOverdueCounters>> COUNTERS = Map.ofEntries(
            Map.entry(new Bucket(1, 3, 1, 15), LegacyOverdueCounters::months1To3Days1To15),
            Map.entry(new Bucket(1, 3, 16, 30), LegacyOverdueCounters::months1To3Days16To30),
            Map.entry(new Bucket(1, 3, 31, null), LegacyOverdueCounters::months1To3DaysFrom31),
            Map.entry(new Bucket(4, 6, 1, 15), LegacyOverdueCounters::months4To6Days1To15),
            Map.entry(new Bucket(4, 6, 16, 30), LegacyOverdueCounters::months4To6Days16To30),
            Map.entry(new Bucket(4, 6, 31, null), LegacyOverdueCounters::months4To6DaysFrom31),
            Map.entry(new Bucket(7, 12, 1, 10), LegacyOverdueCounters::months7To12Days1To10),
            Map.entry(new Bucket(7, 12, 11, 20), LegacyOverdueCounters::months7To12Days11To20),
            Map.entry(new Bucket(7, 12, 21, 30), LegacyOverdueCounters::months7To12Days21To30),
            Map.entry(new Bucket(7, 12, 31, null), LegacyOverdueCounters::months7To12DaysFrom31),
            Map.entry(new Bucket(13, 24, 1, 10), LegacyOverdueCounters::months13To24Days1To10),
            Map.entry(new Bucket(13, 24, 11, 20), LegacyOverdueCounters::months13To24Days11To20),
            Map.entry(new Bucket(13, 24, 21, 30), LegacyOverdueCounters::months13To24Days21To30),
            Map.entry(new Bucket(13, 24, 31, null), LegacyOverdueCounters::months13To24DaysFrom31),
            Map.entry(new Bucket(25, 36, 1, 60), LegacyOverdueCounters::months25To36Days1To60),
            Map.entry(new Bucket(25, 36, 61, 90), LegacyOverdueCounters::months25To36Days61To90),
            Map.entry(new Bucket(25, 36, 91, null), LegacyOverdueCounters::months25To36DaysFrom91)
    );

    /** The counters of the KIB module, field by field; null (no counters) is NONE. */
    public static LegacyOverdueCounters from(OverdueForStopFactor kib) {
        if (kib == null) {
            return NONE;
        }
        return new LegacyOverdueCounters(
                kib.countWherePeriodFrom1To3MonthsAndOverdueFrom1To15(),
                kib.countWherePeriodFrom1To3MonthsAndOverdueFrom16To30(),
                kib.countWherePeriodFrom1To3MonthsAndOverdueFrom31(),
                kib.countWherePeriodFrom4To6MonthsAndOverdueFrom1To15(),
                kib.countWherePeriodFrom4To6MonthsAndOverdueFrom16To30(),
                kib.countWherePeriodFrom4To6MonthsAndOverdueFrom31(),
                kib.countWherePeriodFrom7To12MonthsAndOverdueFrom1To10(),
                kib.countWherePeriodFrom7To12MonthsAndOverdueFrom11To20(),
                kib.countWherePeriodFrom7To12MonthsAndOverdueFrom21To30(),
                kib.countWherePeriodFrom7To12MonthsAndOverdueFrom31(),
                kib.countWherePeriodFrom13To24MonthsAndOverdueFrom1To10(),
                kib.countWherePeriodFrom13To24MonthsAndOverdueFrom11To20(),
                kib.countWherePeriodFrom13To24MonthsAndOverdueFrom21To30(),
                kib.countWherePeriodFrom13To24MonthsAndOverdueFrom31(),
                kib.countWherePeriodFrom25To36MonthsAndOverdueFrom1To60(),
                kib.countWherePeriodFrom25To36MonthsAndOverdueFrom61To90(),
                kib.countWherePeriodFrom25To36MonthsAndOverdueFrom91());
    }

    /**
     * Months with overdue in months periodFrom..periodTo with minDays..maxDays of overdue; empty when no counter has
     * exactly these params.
     */
    public OptionalDouble count(int periodFromMonth, int periodToMonth, int minDays, Integer maxDays) {
        ToIntFunction<LegacyOverdueCounters> counter = COUNTERS.get(new Bucket(periodFromMonth, periodToMonth, minDays, maxDays));
        return counter != null ? OptionalDouble.of(counter.applyAsInt(this)) : OptionalDouble.empty();
    }
}
