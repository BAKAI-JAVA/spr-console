package kg.bakai.spr.core.kib.dto;

/**
 * Overdue counters as the KIB module returns them (KibService.getOverdueByPeriodAndCount): months with overdue by
 * period and overdue days, main debtor contracts only. The stop factor check reads them through
 * {@link LegacyOverdueCounters#from}.
 */
public record OverdueForStopFactor(
        int countWherePeriodFrom1To3MonthsAndOverdueFrom1To15,
        int countWherePeriodFrom1To3MonthsAndOverdueFrom16To30,
        int countWherePeriodFrom1To3MonthsAndOverdueFrom31,
        int countWherePeriodFrom4To6MonthsAndOverdueFrom1To15,
        int countWherePeriodFrom4To6MonthsAndOverdueFrom16To30,
        int countWherePeriodFrom4To6MonthsAndOverdueFrom31,
        int countWherePeriodFrom7To12MonthsAndOverdueFrom1To10,
        int countWherePeriodFrom7To12MonthsAndOverdueFrom11To20,
        int countWherePeriodFrom7To12MonthsAndOverdueFrom21To30,
        int countWherePeriodFrom7To12MonthsAndOverdueFrom31,
        int countWherePeriodFrom13To24MonthsAndOverdueFrom1To10,
        int countWherePeriodFrom13To24MonthsAndOverdueFrom11To20,
        int countWherePeriodFrom13To24MonthsAndOverdueFrom21To30,
        int countWherePeriodFrom13To24MonthsAndOverdueFrom31,
        int countWherePeriodFrom25To36MonthsAndOverdueFrom1To60,
        int countWherePeriodFrom25To36MonthsAndOverdueFrom61To90,
        int countWherePeriodFrom25To36MonthsAndOverdueFrom91
) {

    /** No overdue in any period. */
    public static final OverdueForStopFactor NONE = new OverdueForStopFactor(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
}
