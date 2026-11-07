package kg.bakai.stopfactor.report;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Raw credit history events. A counting calculator uses a list when it is not null and falls back to
 * {@link LegacyCounters} when it is null.
 *
 * @param overdueMonths         months with overdue of the main debtor contracts, null if they cannot be found
 * @param overdueCompleteMonths the overdue months are complete for months 1..N before the request (the KIB payment
 *                              calendars cover them); null - complete for every month
 */
public record CreditHistoryEvents(
        List<@NotNull @Valid OverdueMonth> overdueMonths,
        @Min(1) Integer overdueCompleteMonths,
        List<@NotNull @Valid RestructuringEvent> restructurings,
        List<@NotNull @Min(0) Integer> bureauRequestsDaysAgo,
        List<@NotNull @Min(0) Integer> newCreditsDaysAgo
) {

    /** Only overdue months, complete for every month. */
    public static CreditHistoryEvents ofOverdueMonths(List<OverdueMonth> overdueMonths) {
        return new CreditHistoryEvents(overdueMonths, null, null, null, null);
    }

    /**
     * The overdue months when they are complete for months 1..periodToMonth, otherwise null: a month the KIB
     * payment calendars do not cover may have had overdue that cannot be counted.
     */
    public List<OverdueMonth> overdueMonthsUpTo(int periodToMonth) {
        return overdueCompleteMonths == null || periodToMonth <= overdueCompleteMonths ? overdueMonths : null;
    }

    /**
     * One payment calendar month of a main debtor contract with overdue (KibOverdueHistoryReader). An overdue
     * that goes on into the next month is a month with overdue in both months, as the KIB module counts.
     *
     * @param monthsAgo      the calendar month: 1 - the month of the request, 2 - the month before it, and so on
     * @param maxOverdueDays largest overdue days reached in the month (MaxPastDueDays)
     */
    public record OverdueMonth(
            @Min(1) int monthsAgo,
            @Min(1) int maxOverdueDays
    ) {
    }

    public record RestructuringEvent(
            @NotNull Source source,
            @Min(1) int monthsAgo
    ) {
    }

    public enum Source {
        KIB,
        OB
    }
}
