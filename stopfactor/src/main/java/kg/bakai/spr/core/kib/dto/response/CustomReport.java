package kg.bakai.spr.core.kib.dto.response;

import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.OverdueEpisode;
import kg.bakai.stopfactor.dto.scoring.OverdueForStopFactor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DUMMY of the SPR class with the same name and package (KIB custom report).
 * The stop factor check does not read it, it only passes it to {@link kg.bakai.spr.core.kib.KibService}.
 * <p>
 * LOCAL ONLY: the real report holds the monthly overdue history SPR calculates from. Here it carries what the
 * dummy KibService returns, so the test check endpoint can imitate any KIB history.
 */
@Getter
@Setter
@NoArgsConstructor
public class CustomReport {

    /** Legacy counters returned by KibService.getOverdueByPeriodAndCount, zeros when null. */
    private OverdueForStopFactor overdue;

    /** Returned by KibService.getOverdueEpisodes; null imitates SPR that could not build the episodes. */
    private List<OverdueEpisode> overdueEpisodes = List.of();
}
