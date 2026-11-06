package kg.bakai.stopfactor.service.impl;

import kg.bakai.spr.core.kib.KibService;
import kg.bakai.spr.core.kib.dto.ConsolidationDto;
import kg.bakai.spr.core.kib.dto.GeneralInfoDto;
import kg.bakai.spr.core.kib.dto.IndividualReport;
import kg.bakai.spr.core.kib.dto.LoanDto;
import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.spr.core.ob.dto.TotalDebtReportData;
import kg.bakai.spr.core.ob.dto.response.TotalDebtFinalResponse;
import kg.bakai.stopfactor.calculator.KibOverdueEpisodeCalculator;
import kg.bakai.stopfactor.calculator.KibOverdueEpisodeCalculator.OverdueHistory;
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents;
import kg.bakai.stopfactor.dto.scoring.LegacyCounters;
import kg.bakai.stopfactor.dto.scoring.LegacyOverdueCounters;
import kg.bakai.stopfactor.dto.scoring.ReportValues;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

import static java.time.temporal.ChronoUnit.MONTHS;

/**
 * Turns the KIB and OB reports of an application into the values stop factors are checked with.
 * {@link #values} gives the values of the legacy StopFactorServiceImpl.initializeStopFactorIntegerValueDto, calculated
 * the same way (LegacyStopFactorParityTest), with the fixed-period counters gathered in {@link LegacyCounters}.
 */
@Component
@RequiredArgsConstructor
class StopFactorValuesInitializer {

    private final KibService kibService;
    private final KibOverdueEpisodeCalculator overdueEpisodeCalculator;

    /**
     * Without a full KIB report (custom report, individual report and consolidation) only the current overdue
     * from OB is known, all other values are 0.
     */
    ReportValues values(CustomReport customReport, IndividualReport individualReport,
                        ConsolidationDto consolidationDto, TotalDebtFinalResponse totalDebt, LocalDate requestDate) {
        List<TotalDebtReportData> obCredits = totalDebt.getCredits();
        long obOverdueDays = maxOverdueDays(obCredits);
        double obOverdueAmount = overdueAmount(obCredits);

        if (customReport == null || individualReport == null || consolidationDto == null) {
            return ReportValues.withoutKibReport(obOverdueDays, obOverdueAmount);
        }

        GeneralInfoDto generalInfo = individualReport.generalInfoDto();
        return new ReportValues(
                generalInfo.numberOfActiveDisputes(),
                generalInfo.numberOfUnjustifiedDisputesInPast(),
                Math.max(maxOverOwnLoans(individualReport, LoanDto::getDaysDelay), obOverdueDays),
                consolidationDto.totalOverduePrincipalOrInterestOnAllOpenCreditContracts().doubleValue() + obOverdueAmount,
                consolidationDto.totalOpenCreditContracts(),
                legacyCounters(customReport, individualReport, consolidationDto, obCredits, requestDate));
    }

    private LegacyCounters legacyCounters(CustomReport customReport, IndividualReport individualReport,
                                          ConsolidationDto consolidationDto, List<TotalDebtReportData> obCredits,
                                          LocalDate requestDate) {
        ObChanges obChanges = obChanges(obCredits, requestDate);
        return new LegacyCounters(
                sumOverOwnLoans(individualReport, LoanDto::getNumberOfRestructuringsLast12Months),
                sumOverOwnLoans(individualReport, LoanDto::getNumberOfRestructuringsFrom12To24Months),
                obChanges.months1To12(),
                obChanges.months13To24(),
                individualReport.generalInfoDto().creditInquiriesLast7Days(),
                consolidationDto.totalNumberOfNewOpenLoansInTheLast30Days(),
                LegacyOverdueCounters.from(kibService.getOverdueByPeriodAndCount(customReport, requestDate.atStartOfDay())));
    }

    /**
     * Raw events of the credit history. For now only the overdue episodes, the other lists stay null and their
     * stop factors use the legacy counters.
     * <ul>
     *     <li>without a full KIB report the client has no credit history: no episodes, every overdue count is 0,
     *     like the legacy counters;</li>
     *     <li>the episodes cannot be built from the report (no payment calendars, null): null, the overdue stop
     *     factors use the legacy counters;</li>
     *     <li>the payment calendars do not reach the start of a contract: the episodes are used for the months they
     *     cover only, longer periods use the legacy counters.</li>
     * </ul>
     */
    CreditHistoryEvents history(CustomReport customReport, IndividualReport individualReport,
                                ConsolidationDto consolidationDto, LocalDate requestDate) {
        if (customReport == null || individualReport == null || consolidationDto == null) {
            return CreditHistoryEvents.ofOverdueEpisodes(List.of());
        }
        OverdueHistory overdue = overdueEpisodeCalculator.getOverdueHistory(customReport, requestDate.atStartOfDay());
        if (overdue == null) {
            return null;
        }
        return new CreditHistoryEvents(List.copyOf(overdue.episodes()), overdue.completeMonths(), null, null, null);
    }

    /**
     * Statuses of the loans where the client is the main debtor or a guarantor, in report order; empty without
     * an individual report.
     */
    List<String> loanStatuses(IndividualReport individualReport) {
        if (individualReport == null) {
            return List.of();
        }
        return individualReport.loanDtoList().stream()
                .filter(StopFactorValuesInitializer::isMainDebtorOrGuarantor)
                .map(LoanDto::getLoanStatus)
                .filter(Objects::nonNull)
                .map(Enum::name)
                .toList();
    }

    /**
     * Only loans where the client is the main debtor or a guarantor count (not co-debtor loans).
     */
    static boolean isMainDebtorOrGuarantor(LoanDto loan) {
        return loan.getParticipationType() == RoleOfClient.MainDebtor || loan.getParticipationType() == RoleOfClient.Guarantor;
    }

    // ------------------------------------------------------------------------------------------------
    // KIB
    // ------------------------------------------------------------------------------------------------

    /** Sum of a value over the loans of the client as main debtor or guarantor. */
    private static int sumOverOwnLoans(IndividualReport individualReport, ToIntFunction<LoanDto> value) {
        return individualReport.loanDtoList().stream()
                .filter(StopFactorValuesInitializer::isMainDebtorOrGuarantor)
                .mapToInt(value)
                .sum();
    }

    /** Maximum of a value over the loans of the client as main debtor or guarantor, at least 0, as in legacy. */
    private static int maxOverOwnLoans(IndividualReport individualReport, ToIntFunction<LoanDto> value) {
        int max = individualReport.loanDtoList().stream()
                .filter(StopFactorValuesInitializer::isMainDebtorOrGuarantor)
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
