package kg.bakai.stopfactor.service.impl;

import kg.bakai.spr.core.kib.KibService;
import kg.bakai.spr.core.kib.dto.ConsolidationDto;
import kg.bakai.spr.core.kib.dto.IndividualReport;
import kg.bakai.spr.core.kib.dto.LoanDto;
import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.spr.core.ob.dto.TotalDebtReportData;
import kg.bakai.spr.core.ob.dto.response.TotalDebtFinalResponse;
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents;
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.OverdueEpisode;
import kg.bakai.stopfactor.dto.scoring.StopFactorIntegerValueDto;
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
 * {@link #values} is the legacy StopFactorServiceImpl.initializeStopFactorIntegerValueDto, the logic is not changed.
 */
@Component
@RequiredArgsConstructor
class StopFactorValuesInitializer {

    private final KibService kibService;

    /**
     * Without a full KIB report (custom report, individual report and consolidation) only the current overdue
     * from OB is known, all other values are 0.
     */
    StopFactorIntegerValueDto values(CustomReport customReport, IndividualReport individualReport,
                                     ConsolidationDto consolidationDto, TotalDebtFinalResponse totalDebt,
                                     LocalDate requestDate) {
        List<TotalDebtReportData> obCredits = totalDebt.getCredits();
        long obOverdueDays = getMaxOverdueDays(obCredits);
        double obOverdueSum = getOverdueSum(obCredits);

        if (customReport == null || individualReport == null || consolidationDto == null) {
            return new StopFactorIntegerValueDto(obOverdueDays, obOverdueSum);
        }

        CountOfChanges countOfChangesOb = getCountOfChangesOb(obCredits, requestDate);
        var overdue = kibService.getOverdueByPeriodAndCount(customReport, requestDate.atStartOfDay());
        return new StopFactorIntegerValueDto(
                individualReport.generalInfoDto().numberOfActiveDisputes(),
                individualReport.generalInfoDto().numberOfUnjustifiedDisputesInPast(),
                getSumOfParamsWhereCustomerIsMainDebtorOrGuarantor(individualReport, LoanDto::getNumberOfRestructuringsLast12Months),
                countOfChangesOb.countOfChangesObLast12Month(),
                getSumOfParamsWhereCustomerIsMainDebtorOrGuarantor(individualReport, LoanDto::getNumberOfRestructuringsFrom12To24Months),
                countOfChangesOb.countOfChangesObFrom13To24Months(),
                individualReport.generalInfoDto().creditInquiriesLast7Days(),
                Math.max(getMaxOfParamsWhereCustomerIsMainDebtorOrGuarantor(individualReport, LoanDto::getDaysDelay), obOverdueDays),
                consolidationDto.totalOverduePrincipalOrInterestOnAllOpenCreditContracts().doubleValue() + obOverdueSum,
                consolidationDto.totalNumberOfNewOpenLoansInTheLast30Days(),
                consolidationDto.totalOpenCreditContracts(),
                overdue
        );
    }

    /**
     * Raw events of the credit history. For now only the overdue episodes, the other lists stay null and their
     * stop factors use the legacy values.
     * <ul>
     *     <li>without a full KIB report the client has no credit history: no episodes, every overdue count is 0,
     *     like the legacy values;</li>
     *     <li>SPR could not build the episodes (null): null, the overdue stop factors use the legacy counters.</li>
     * </ul>
     */
    CreditHistoryEvents history(CustomReport customReport, IndividualReport individualReport,
                                          ConsolidationDto consolidationDto, LocalDate requestDate) {
        if (customReport == null || individualReport == null || consolidationDto == null) {
            return new CreditHistoryEvents(List.of(), null, null, null);
        }
        List<OverdueEpisode> episodes = kibService.getOverdueEpisodes(customReport, requestDate.atStartOfDay());
        return episodes != null ? new CreditHistoryEvents(List.copyOf(episodes), null, null, null) : null;
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

    private static int getSumOfParamsWhereCustomerIsMainDebtorOrGuarantor(IndividualReport individualReport, ToIntFunction<LoanDto> variableFunction) {
        return individualReport.loanDtoList().stream()
                .filter(StopFactorValuesInitializer::isMainDebtorOrGuarantor)
                .mapToInt(variableFunction)
                .sum();
    }

    // The maximum starts from 0, as in legacy
    private static int getMaxOfParamsWhereCustomerIsMainDebtorOrGuarantor(IndividualReport individualReport, ToIntFunction<LoanDto> variableFunction) {
        int max = individualReport.loanDtoList().stream()
                .filter(StopFactorValuesInitializer::isMainDebtorOrGuarantor)
                .mapToInt(variableFunction)
                .max()
                .orElse(0);
        return Math.max(0, max);
    }

    // ------------------------------------------------------------------------------------------------
    // OB
    // ------------------------------------------------------------------------------------------------

    private static long getMaxOverdueDays(List<TotalDebtReportData> credits) {
        return credits.stream().mapToLong(TotalDebtReportData::getCurrentOverdueDaysCount).max().orElse(0);
    }

    // A credit without an overdue sum counts as 0
    private static double getOverdueSum(List<TotalDebtReportData> credits) {
        return credits.stream()
                .map(TotalDebtReportData::getCurrentOverdueSum)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
    }

    // Changes of OB credits by the months between the last change and the request; credits never changed are skipped
    private static CountOfChanges getCountOfChangesOb(List<TotalDebtReportData> credits, LocalDate requestDate) {
        int countOfChangesObLast12Month = 0;
        int countOfChangesObFrom13To24Months = 0;

        for (TotalDebtReportData item : credits) {
            if (item.getLastChangeDate() == null) {
                continue;
            }
            long months = MONTHS.between(item.getLastChangeDate(), requestDate);
            if (months <= 12) {
                countOfChangesObLast12Month++;
            } else if (months <= 24) {
                countOfChangesObFrom13To24Months++;
            }
        }
        return new CountOfChanges(countOfChangesObLast12Month, countOfChangesObFrom13To24Months);
    }

    private record CountOfChanges(int countOfChangesObLast12Month, int countOfChangesObFrom13To24Months) {
    }
}
