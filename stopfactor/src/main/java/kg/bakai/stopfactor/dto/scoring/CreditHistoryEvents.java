package kg.bakai.stopfactor.dto.scoring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Raw credit history events. A calculator uses a list when it is not null and falls back to the legacy values of
 * {@link StopFactorIntegerValueDto} when it is null.
 */
public record CreditHistoryEvents(
        List<@NotNull @Valid OverdueEpisode> overdueEpisodes,
        List<@NotNull @Valid RestructuringEvent> restructurings,
        List<@NotNull @Min(0) Integer> bureauRequestsDaysAgo,
        List<@NotNull @Min(0) Integer> newCreditsDaysAgo
) {

    /**
     * One continuous overdue, see KibService.getOverdueEpisodes in SPR.
     *
     * @param monthsAgo      month the maximum overdue was reached, counted back from the request (1 - the last month)
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
