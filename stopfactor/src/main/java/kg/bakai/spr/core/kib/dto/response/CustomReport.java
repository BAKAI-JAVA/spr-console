package kg.bakai.spr.core.kib.dto.response;

import kg.bakai.stopfactor.dto.scoring.OverdueForStopFactor;

/**
 * DUMMY of the SPR class with the same name and package (KIB custom report). Only what the stop factor check reads.
 *
 * @param contracts contracts with their payment calendars, the source of the overdue episodes
 *                  (KibOverdueEpisodeCalculator)
 * @param overdue   LOCAL ONLY: the legacy counters the dummy KibService.getOverdueByPeriodAndCount returns;
 *                  in SPR they are calculated from the report
 */
public record CustomReport(Contracts contracts, OverdueForStopFactor overdue) {

    /** A report without contracts: the client has no overdue history. */
    public CustomReport() {
        this(null, null);
    }
}
