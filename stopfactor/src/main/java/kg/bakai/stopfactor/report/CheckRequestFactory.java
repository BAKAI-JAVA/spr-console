package kg.bakai.stopfactor.report;

import kg.bakai.spr.core.kib.KibService;
import kg.bakai.spr.core.kib.dto.ConsolidationDto;
import kg.bakai.spr.core.kib.dto.GeneralInfoDto;
import kg.bakai.spr.core.kib.dto.IndividualReport;
import kg.bakai.spr.core.kib.dto.LoanDto;
import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import kg.bakai.spr.core.ob.dto.TotalDebtReportData;
import kg.bakai.stopfactor.report.KibOverdueHistoryReader.OverdueHistory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

import static java.time.temporal.ChronoUnit.MONTHS;

/**
 * Builds the {@link StopFactorCheckRequest} of an application from its KIB and OB reports: everything a calculator
 * reads, so calculators never touch the reports themselves.
 * <ul>
 *     <li>{@link ReportValues} - the values of the legacy StopFactorServiceImpl.initializeStopFactorIntegerValueDto,
 *     calculated the same way (LegacyStopFactorParityTest), with the fixed-period counters in {@link LegacyCounters};</li>
 *     <li>{@link CreditHistoryEvents} - the months with overdue of the KIB payment calendars;</li>
 *     <li>the KIB loan statuses of the client.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CheckRequestFactory {

    private final KibService kibService;
    private final KibOverdueHistoryReader overdueHistoryReader;

    public StopFactorCheckRequest create(long scoringId, Long productId, ScoringReports reports) {
        return new StopFactorCheckRequest(scoringId, productId, values(reports), history(reports),
                loanStatuses(reports.individualReport()));
    }

    /** Without a full KIB report only the current overdue from OB is known, all other values are 0. */
    private ReportValues values(ScoringReports reports) {
        List<TotalDebtReportData> obCredits = reports.totalDebt().getCredits();
        long obOverdueDays = maxOverdueDays(obCredits);
        double obOverdueAmount = overdueAmount(obCredits);
        if (!reports.hasKibReport()) {
            return ReportValues.withoutKibReport(obOverdueDays, obOverdueAmount);
        }

        IndividualReport individualReport = reports.individualReport();
        ConsolidationDto consolidation = reports.consolidation();
        GeneralInfoDto generalInfo = individualReport.generalInfoDto();
        return new ReportValues(
                generalInfo.numberOfActiveDisputes(),
                generalInfo.numberOfUnjustifiedDisputesInPast(),
                Math.max(maxOverOwnLoans(individualReport, LoanDto::getDaysDelay), obOverdueDays),
                consolidation.totalOverduePrincipalOrInterestOnAllOpenCreditContracts().doubleValue() + obOverdueAmount,
                consolidation.totalOpenCreditContracts(),
                legacyCounters(reports, obCredits));
    }

    private LegacyCounters legacyCounters(ScoringReports reports, List<TotalDebtReportData> obCredits) {
        IndividualReport individualReport = reports.individualReport();
        ObChanges obChanges = obChanges(obCredits, reports.requestDate());
        return new LegacyCounters(
                sumOverOwnLoans(individualReport, LoanDto::getNumberOfRestructuringsLast12Months),
                sumOverOwnLoans(individualReport, LoanDto::getNumberOfRestructuringsFrom12To24Months),
                obChanges.months1To12(),
                obChanges.months13To24(),
                individualReport.generalInfoDto().creditInquiriesLast7Days(),
                reports.consolidation().totalNumberOfNewOpenLoansInTheLast30Days(),
                LegacyOverdueCounters.from(kibService.getOverdueByPeriodAndCount(reports.customReport(),
                        reports.requestDate().atStartOfDay())));
    }

    /**
     * Raw events of the credit history. For now only the overdue months, the other lists stay null and their
     * stop factors use the legacy counters.
     * <ul>
     *     <li>without a full KIB report the client has no credit history: no overdue months, every overdue count
     *     is 0, like the legacy counters;</li>
     *     <li>the overdue months cannot be found in the report (no payment calendars): null, the overdue stop
     *     factors use the legacy counters;</li>
     *     <li>the payment calendars do not reach the start of a contract: the overdue months are used for the months
     *     they cover only, longer periods use the legacy counters.</li>
     * </ul>
     */
    private CreditHistoryEvents history(ScoringReports reports) {
        if (!reports.hasKibReport()) {
            return CreditHistoryEvents.ofOverdueMonths(List.of());
        }
        OverdueHistory overdue = overdueHistoryReader.read(reports.customReport(), reports.requestDate().atStartOfDay());
        if (overdue == null) {
            return null;
        }
        return new CreditHistoryEvents(List.copyOf(overdue.months()), overdue.completeMonths(), null, null, null);
    }

    /**
     * Statuses of the loans where the client is the main debtor or a guarantor, in report order; empty without
     * an individual report.
     */
    private static List<String> loanStatuses(IndividualReport individualReport) {
        if (individualReport == null) {
            return List.of();
        }
        return individualReport.loanDtoList().stream()
                .filter(CheckRequestFactory::isMainDebtorOrGuarantor)
                .map(LoanDto::getLoanStatus)
                .filter(Objects::nonNull)
                .map(Enum::name)
                .toList();
    }

    /**
     * Only loans where the client is the main debtor or a guarantor count (not co-debtor loans).
     */
    private static boolean isMainDebtorOrGuarantor(LoanDto loan) {
        return loan.getParticipationType() == RoleOfClient.MainDebtor || loan.getParticipationType() == RoleOfClient.Guarantor;
    }

    // ------------------------------------------------------------------------------------------------
    // KIB
    // ------------------------------------------------------------------------------------------------

    /** Sum of a value over the loans of the client as main debtor or guarantor. */
    private static int sumOverOwnLoans(IndividualReport individualReport, ToIntFunction<LoanDto> value) {
        return individualReport.loanDtoList().stream()
                .filter(CheckRequestFactory::isMainDebtorOrGuarantor)
                .mapToInt(value)
                .sum();
    }

    /** Maximum of a value over the loans of the client as main debtor or guarantor, at least 0, as in legacy. */
    private static int maxOverOwnLoans(IndividualReport individualReport, ToIntFunction<LoanDto> value) {
        int max = individualReport.loanDtoList().stream()
                .filter(CheckRequestFactory::isMainDebtorOrGuarantor)
                .mapToInt(value)
                .max()
                .orElse(0);
        return Math.max(0, max);
    }

    // ------------------------------------------------------------------------------------------------
    // OB
    // ------------------------------------------------------------------------------------------------

    private static long maxOverdueDays(List<TotalDebtReportData> credits) {
        return credits.stream().mapToLong(TotalDebtReportData::getCurrentOverdueDaysCount).max().orElse(0);
    }

    // A credit without an overdue sum counts as 0
    private static double overdueAmount(List<TotalDebtReportData> credits) {
        return credits.stream()
                .map(TotalDebtReportData::getCurrentOverdueSum)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
    }

    /** OB credits by the months between their last change and the request; credits never changed are skipped. */
    private static ObChanges obChanges(List<TotalDebtReportData> credits, LocalDate requestDate) {
        int months1To12 = 0;
        int months13To24 = 0;
        for (TotalDebtReportData credit : credits) {
            if (credit.getLastChangeDate() == null) {
                continue;
            }
            long months = MONTHS.between(credit.getLastChangeDate(), requestDate);
            if (months <= 12) {
                months1To12++;
            } else if (months <= 24) {
                months13To24++;
            }
        }
        return new ObChanges(months1To12, months13To24);
    }

    private record ObChanges(int months1To12, int months13To24) {
    }
}
