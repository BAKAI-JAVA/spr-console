package kg.bakai.spr.core.kib.dto.response;

import kg.bakai.spr.core.kib.dto.OverdueForStopFactor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DUMMY of the SPR class with the same name and package: the KIB custom report (CreditinfoReportPlus). Only what the
 * stop factor check reads, mapped from the KIB JSON names.
 *
 * @param contracts contracts with their payment calendars, the source of the overdue months
 *                  (KibOverdueHistoryReader)
 * @param overdue   LOCAL ONLY, not in the KIB JSON: the legacy counters the dummy
 *                  KibService.getOverdueByPeriodAndCount returns; in SPR they are calculated from the report
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomReport(
        @JsonProperty("Contracts") Contracts contracts,
        @JsonIgnore OverdueForStopFactor overdue
) {

    /** A report without contracts: the client has no overdue history. */
    public CustomReport() {
        this(null, null);
    }
}
