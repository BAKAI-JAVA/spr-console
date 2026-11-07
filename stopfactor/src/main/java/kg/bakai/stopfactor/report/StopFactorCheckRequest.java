package kg.bakai.stopfactor.report;

import java.util.List;

/**
 * What a stop factor calculator reads for one application.
 *
 * @param scoringId    scoring being checked
 * @param productId    its product, null if the scoring has none
 * @param values       values of the KIB / OB reports, with the legacy counters
 * @param history      raw credit history events, null if none can be built; a list of it is used when not null
 * @param loanStatuses statuses of the KIB loans of the client (main debtor or guarantor), in report order
 */
public record StopFactorCheckRequest(
        long scoringId,
        Long productId,
        ReportValues values,
        CreditHistoryEvents history,
        List<String> loanStatuses
) {
}
