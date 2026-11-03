package kg.bakai.spr.core.kib.dto.enums;

/**
 * DUMMY of the SPR class with the same name and package. Only what the stop factor check uses.
 * Values from the stop factor 1 conditions (stop-factor-condition.csv) plus the usual non-refusal statuses.
 */
public enum LoanStatus {
    Open,
    FullyPaid,
    WrittenOff,
    SoldToThirdParty,
    OpenUnderLegalProceedings,
    FullyPaidCollateralExecutionThroughCourt,
    WrittenOffThroughLoanLossProvision,
    FullyPaidResolvedThroughCourt,
    FullyPaidCollateralExecution,
    TransferredToOtherReportingInst
}
