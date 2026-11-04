package kg.bakai.stopfactor.dto.scoring;

/**
 * What a stop factor calculator reads for one application.
 *
 * @param scoringId scoring being checked
 * @param productId its product, null if the scoring has none
 * @param values    values of the KIB / OB reports, the legacy source of every calculator
 * @param history   raw credit history events; a calculator uses a list of it when the list is not null
 */
public record StopFactorCheckRequest(
        long scoringId,
        Long productId,
        StopFactorIntegerValueDto values,
        CreditHistoryEvents history
) {
}
