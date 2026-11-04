package kg.bakai.stopfactor.dto.scoring;

public record StopFactorIntegerValueDto(
        double numberOfActiveDisputes,
        double numberOfUnjustifiedDisputesInPast,
        double numberOfRestructuringsLast12Months,
        double countOfChangesObLast12Month,
        double numberOfRestructuringsFrom12To24Months,
        double countOfChangesObFrom13To24Months,
        double creditInquiriesLast7Days,
        double currentNumberOfDaysOverdue,
        double currentAmountOfOverdue,
        double totalNumberOfNewOpenLoansInTheLast30Days,
        double totalOpenCreditContracts,
        OverdueForStopFactor overdue
) {
    /** Values without a KIB report: only the current overdue from OB is known, everything else is 0. */
    public StopFactorIntegerValueDto(double currentNumberOfDaysOverdue, double currentAmountOfOverdue) {
        this(0, 0, 0, 0, 0, 0, 0, currentNumberOfDaysOverdue, currentAmountOfOverdue, 0, 0, OverdueForStopFactor.NONE);
    }
}
