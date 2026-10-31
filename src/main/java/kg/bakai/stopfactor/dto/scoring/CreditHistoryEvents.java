package kg.bakai.stopfactor.dto.scoring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreditHistoryEvents(
        List<@NotNull @Valid OverdueEpisode> overdueEpisodes,
        List<@NotNull @Valid RestructuringEvent> restructurings,
        List<@NotNull @Min(0) Integer> bureauRequestsDaysAgo,
        List<@NotNull @Min(0) Integer> newCreditsDaysAgo
) {

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
