package kg.bakai.spr.core.kib.dto;

import java.math.BigDecimal;

/**
 * DUMMY of the SPR class with the same name and package (KIB consolidation). Only what the stop factor check uses.
 */
public record ConsolidationDto(
        BigDecimal totalOverduePrincipalOrInterestOnAllOpenCreditContracts,
        int totalNumberOfNewOpenLoansInTheLast30Days,
        int totalOpenCreditContracts
) {
}
