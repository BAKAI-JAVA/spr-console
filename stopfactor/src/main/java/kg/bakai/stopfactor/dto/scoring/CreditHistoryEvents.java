package kg.bakai.stopfactor.dto.scoring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Raw credit history events. A counting calculator uses a list when it is not null and falls back to
 * {@link LegacyCounters} when it is null.
 *
 * @param overdueEpisodes       continuous overdues, null if they cannot be built
 * @param overdueEpisodesMonths the episodes are complete for months 1..N before the request (the KIB payment
 *                              calendars cover them); null - complete for every month
 */
public record CreditHistoryEvents(
        List<@NotNull @Valid OverdueEpisode> overdueEpisodes,
        @Min(1) Integer overdueEpisodesMonths,
        List<@NotNull @Valid RestructuringEvent> restructurings,
        List<@NotNull @Min(0) Integer> bureauRequestsDaysAgo,
        List<@NotNull @Min(0) Integer> newCreditsDaysAgo
) {

    /** Only overdue episodes, complete for every month. */
    public static CreditHistoryEvents ofOverdueEpisodes(List<OverdueEpisode> overdueEpisodes) {
        return new CreditHistoryEvents(overdueEpisodes, null, null, null, null);
    }

    /**
     * The overdue episodes when they are complete for months 1..periodToMonth, otherwise null: a month the KIB
     * payment calendars do not cover may have had overdue, so the episodes cannot count it.
     */
    public List<OverdueEpisode> overdueEpisodesUpTo(int periodToMonth) {
        return overdueEpisodesMonths == null || periodToMonth <= overdueEpisodesMonths ? overdueEpisodes : null;
    }

    /**
     * One continuous overdue, built by KibOverdueEpisodeCalculator from the KIB payment calendars.
     *
     * @param monthsAgo      calendar month the maximum overdue was reached: 1 - the month of the request, 2 - the
     *                       month before it, and so on
     * @param maxOverdueDays largest number of overdue days in the episode
     */
    public record OverdueEpisode(
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
