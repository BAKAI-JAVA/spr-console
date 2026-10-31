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
    public StopFactorIntegerValueDto(
            double currentNumberOfDaysOverdue,
            double currentAmountOfOverdue
    ) {
        this(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                currentNumberOfDaysOverdue,
                currentAmountOfOverdue,
                0,
                0,
                new OverdueForStopFactor(
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                )
        );
    }
}