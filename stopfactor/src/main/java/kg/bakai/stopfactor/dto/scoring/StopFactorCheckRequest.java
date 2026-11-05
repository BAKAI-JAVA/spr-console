package kg.bakai.stopfactor.dto.scoring;

import java.util.List;

/**
 * What a stop factor calculator reads for one application.
 *
 * @param scoringId scoring being checked
 * @param productId its product, null if the scoring has none
 * @param values       values of the KIB / OB reports, the legacy source of every calculator
 * @param history      raw credit history events; a calculator uses a list of it when the list is not null
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
