package kg.bakai.spr.core.kib;

import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.OverdueEpisode;
import kg.bakai.stopfactor.dto.scoring.OverdueForStopFactor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DUMMY of the SPR service with the same name and package. Only the methods the stop factor check uses.
 */
@Service
public class KibService {

    /**
     * Legacy fixed overdue counters. The real implementation counts overdue episodes of the KIB custom report;
     * this one returns the counters the dummy report carries, zeros by default.
     */
    public OverdueForStopFactor getOverdueByPeriodAndCount(CustomReport customReport, LocalDateTime requestDate) {
        return customReport.getOverdue() != null ? customReport.getOverdue() : OverdueForStopFactor.NONE;
    }

    /**
     * TO IMPLEMENT IN SPR: continuous overdue episodes of principal and/or interest from the KIB custom report,
     * the source of every OVERDUE_BUCKET_COUNT stop factor (any period and overdue days range).
     * <p>
     * Contract:
     * <ul>
     *     <li>only loans where the client is the main debtor or a guarantor, like the other KIB values;</li>
     *     <li>one episode per continuous overdue of a loan: from the first day with overdue to the day it is paid;</li>
     *     <li>{@code maxOverdueDays} - the largest number of overdue days the episode reached;</li>
     *     <li>{@code monthsAgo} - the month the episode reached {@code maxOverdueDays} (the latest such month if
     *     reached more than once), counted back from the request date: 1 - less than a month before it,
     *     2 - one to two months before it, and so on. The episode is counted once, in the period of that month;</li>
     *     <li>an episode still open on the request date counts with the overdue days reached by that date.</li>
     * </ul>
     *
     * @return the episodes, empty if the client never had overdue, or null if they cannot be built from the
     * report - then the check falls back to {@link #getOverdueByPeriodAndCount} and a period / range without a
     * legacy counter sends the application to the underwriter
     */
    public List<OverdueEpisode> getOverdueEpisodes(CustomReport customReport, LocalDateTime requestDate) {
        // dummy: the episodes the dummy report carries (no overdue by default, null when the test imitates a failure)
        return customReport.getOverdueEpisodes();
    }
}
