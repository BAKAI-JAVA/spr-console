package kg.bakai.stopfactor.dto.scoring;

import java.util.Objects;

/**
 * Values of the KIB / OB reports of one application.
 *
 * @param activeDisputes       active disputes in KIB
 * @param unjustifiedDisputes  unjustified disputes in the past in KIB
 * @param currentOverdueDays   largest current overdue in days, KIB loans of the client and OB credits
 * @param currentOverdueAmount current overdue amount, KIB consolidation and OB credits
 * @param openCredits          open credit contracts in KIB
 * @param legacy               fixed-period counters of the old algorithm, the fallback of the counting stop factors
 */
public record ReportValues(
        double activeDisputes,
        double unjustifiedDisputes,
        double currentOverdueDays,
        double currentOverdueAmount,
        double openCredits,
        LegacyCounters legacy
) {

    public ReportValues {
        Objects.requireNonNull(legacy, "legacy");
    }

    /** Without a KIB report only the current overdue from OB is known; everything else is 0. */
    public static ReportValues withoutKibReport(double currentOverdueDays, double currentOverdueAmount) {
        return new ReportValues(0, 0, currentOverdueDays, currentOverdueAmount, 0, LegacyCounters.NONE);
    }
}
