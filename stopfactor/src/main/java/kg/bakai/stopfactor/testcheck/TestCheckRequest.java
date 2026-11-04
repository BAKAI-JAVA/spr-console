package kg.bakai.stopfactor.testcheck;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import kg.bakai.spr.core.kib.dto.enums.LoanStatus;
import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.OverdueEpisode;
import kg.bakai.stopfactor.dto.scoring.OverdueForStopFactor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Application for the test check: the KIB and OB data SPR would pass to StopFactorService.check, in a short form.
 * Every field is optional; an omitted number is 0.
 *
 * @param scoringId   scoring the results are saved for; omitted - a new negative test id
 * @param productId   links the scoring to this product (public.scoring), so its segment and product limits apply;
 *                    omitted - the scoring keeps its product (scorings 1..5 of changelog 1.2), or has none
 * @param requestDate date of the application, today when omitted
 * @param kib         KIB report; null - the client has no KIB report
 * @param obCredits   credits of the OB report (Открытый банк)
 */
public record TestCheckRequest(
        Long scoringId,
        Long productId,
        LocalDate requestDate,
        @Valid Kib kib,
        List<@Valid ObCredit> obCredits
) {

    /**
     * @param overdueEpisodes     continuous overdue episodes SPR builds from the report (KibService.getOverdueEpisodes)
     * @param episodesUnavailable true imitates SPR that could not build the episodes (returns null)
     * @param legacyOverdue       the 17 legacy counters (KibService.getOverdueByPeriodAndCount), zeros when omitted
     * @param missingParts        parts of the KIB report SPR did not receive
     */
    public record Kib(
            int activeDisputes,
            int unjustifiedDisputes,
            int bureauRequestsLast7Days,
            BigDecimal overdueAmount,
            int newCreditsLast30Days,
            int openCredits,
            List<Loan> loans,
            List<OverdueEpisode> overdueEpisodes,
            boolean episodesUnavailable,
            OverdueForStopFactor legacyOverdue,
            Set<KibPart> missingParts
    ) {
    }

    @Schema(description = "A part of the KIB report: without any of them the check treats the client as having no KIB report")
    public enum KibPart {
        CUSTOM_REPORT,
        INDIVIDUAL_REPORT,
        CONSOLIDATION
    }

    /**
     * @param role   MainDebtor when omitted
     * @param status Open when omitted
     */
    public record Loan(
            RoleOfClient role,
            LoanStatus status,
            int restructuringsLast12Months,
            int restructuringsFrom12To24Months,
            int daysDelay
    ) {
    }

    public record ObCredit(
            long overdueDays,
            BigDecimal overdueSum,
            LocalDate lastChangeDate
    ) {
    }
}
