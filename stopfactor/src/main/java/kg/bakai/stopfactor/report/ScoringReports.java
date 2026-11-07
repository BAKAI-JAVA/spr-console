package kg.bakai.stopfactor.report;

import kg.bakai.spr.core.kib.dto.ConsolidationDto;
import kg.bakai.spr.core.kib.dto.IndividualReport;
import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.spr.core.ob.dto.response.TotalDebtFinalResponse;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The reports SPR checks an application with.
 *
 * @param customReport     KIB custom report, null without a KIB report
 * @param individualReport KIB individual report, null without a KIB report
 * @param consolidation    KIB consolidation, null without a KIB report
 * @param totalDebt        OB total debt, always there
 * @param requestDate      date of the application
 */
public record ScoringReports(
        CustomReport customReport,
        IndividualReport individualReport,
        ConsolidationDto consolidation,
        TotalDebtFinalResponse totalDebt,
        LocalDate requestDate
) {

    public ScoringReports {
        Objects.requireNonNull(totalDebt, "totalDebt");
        Objects.requireNonNull(requestDate, "requestDate");
    }

    /** All three KIB parts are there; with any of them missing the client is checked as having no KIB report. */
    public boolean hasKibReport() {
        return customReport != null && individualReport != null && consolidation != null;
    }
}
