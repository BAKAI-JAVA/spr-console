package kg.bakai.stopfactor.service.impl;

import kg.bakai.common.exception.BusinessValidationException;
import kg.bakai.spr.core.kib.dto.ConsolidationDto;
import kg.bakai.spr.core.kib.dto.IndividualReport;
import kg.bakai.spr.core.kib.dto.LoanDto;
import kg.bakai.spr.core.kib.dto.enums.LoanStatus;
import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.spr.core.ob.dto.response.TotalDebtFinalResponse;
import kg.bakai.stopfactor.calculator.CalculatorRegistry;
import kg.bakai.stopfactor.calculator.CheckedValue;
import kg.bakai.stopfactor.dto.console.ConditionDto;
import kg.bakai.stopfactor.dto.console.StopFactorRequest;
import kg.bakai.stopfactor.dto.console.StopFactorResponse;
import kg.bakai.stopfactor.dto.scoring.*;
import kg.bakai.stopfactor.entity.StopFactor;
import kg.bakai.stopfactor.entity.StopFactorCheck;
import kg.bakai.stopfactor.entity.StopFactorCondition;
import kg.bakai.stopfactor.entity.StopFactorMetric;
import kg.bakai.stopfactor.enums.CalculatorType;
import kg.bakai.stopfactor.enums.InternalStatusCode;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.repository.StopFactorCheckRepository;
import kg.bakai.stopfactor.repository.StopFactorConditionRepository;
import kg.bakai.stopfactor.repository.StopFactorMetricRepository;
import kg.bakai.stopfactor.repository.StopFactorRepository;
import kg.bakai.stopfactor.repository.projection.ProductProjection;
import kg.bakai.stopfactor.repository.projection.StopFactorProjection;
import kg.bakai.stopfactor.service.StopFactorService;
import kg.bakai.stopfactor.service.console.StopFactorConsoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static kg.bakai.stopfactor.enums.StopFactorStatus.*;

/**
 * Stop factor check of SPR: the legacy StopFactorServiceImpl (see StopFactorLegacyServiceImpl), working with the
 * versioned stop factors of migration 004.
 * <p>
 * How an application is checked:
 * <ol>
 *     <li>the KIB/OB reports become values ({@link StopFactorValuesInitializer}, unchanged legacy code);</li>
 *     <li>for every active stop factor (current version, applicable to the scoring's product) its value is taken
 *     ({@link #getVariableFunction}), then its REJECTED condition is checked, then UNDERWRITER if not rejected
 *     ({@link #getStopFactorCheck});</li>
 *     <li>any REJECTED rejects the application (451 if a current overdue stop factor rejected, otherwise code
 *     null), otherwise any UNDERWRITER sends it to the underwriter;</li>
 *     <li>the results are saved for the scoring.</li>
 * </ol>
 * What differs from legacy, because after migration 004 every console edit creates a version with a new id:
 * <ul>
 *     <li>the value comes from the stop factor's metric and params, not from a switch over ids 1..34;</li>
 *     <li>451 is chosen by the metric (current overdue days / amount), not by ids 9 and 10;</li>
 *     <li>a stop factor whose value cannot be calculated goes to the underwriter as {@link #NO_DATA};</li>
 *     <li>stop factors limited to other products are skipped (public.scoring -> public.product).</li>
 * </ul>
 * Migrated stop factors 1..34 give exactly the legacy results (LegacyStopFactorParityTest).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StopFactorServiceImpl implements StopFactorService {

    /**
     * Checked value of a stop factor that cannot be calculated from the reports (e.g. a period set in the console
     * that the report values do not cover). Such a stop factor sends the application to the underwriter.
     */
    public static final String NO_DATA = "NO_DATA";

    static final String NO_MATCHES = "Нет совпадений";

    // Who created a stop factor or condition through save() and saveCondition()
    static final String SPR_USER = "spr";

    // Legacy stop factors 9 (current overdue days) and 10 (current overdue amount) give code 451
    private static final Set<CalculatorType> CURRENT_OVERDUE_CALCULATORS =
            EnumSet.of(CalculatorType.CURRENT_OVERDUE_DAYS, CalculatorType.CURRENT_OVERDUE_AMOUNT);

    private final StopFactorRepository stopFactorRepository;
    private final StopFactorConditionRepository stopFactorConditionRepository;
    private final StopFactorCheckRepository stopFactorCheckRepository;
    private final StopFactorMetricRepository stopFactorMetricRepository;
    private final StopFactorConsoleService stopFactorConsoleService;
    private final StopFactorValuesInitializer stopFactorValuesInitializer;
    private final CalculatorRegistry calculatorRegistry;

    // ================================================================================================
    // StopFactorService
    // ================================================================================================

    @Override
    @Transactional
    public StopFactorDecision check(
            long scoringId,
            CustomReport customReport, IndividualReport individualReport,
            ConsolidationDto consolidationDto, TotalDebtFinalResponse totalDebt, LocalDate requestDate
    ) {
        StopFactorIntegerValueDto stopFactorIntegerValueDto = stopFactorValuesInitializer.initializeStopFactorIntegerValueDto(
                customReport, individualReport, consolidationDto, totalDebt, requestDate);
        Long productId = stopFactorRepository.findProductByScoringId(scoringId).map(ProductProjection::getId).orElse(null);
        StopFactorCheckRequest request = new StopFactorCheckRequest(scoringId, productId, null, stopFactorIntegerValueDto, null);

        List<StopFactor> stopFactors = findAllActive(productId);
        Map<Long, List<StopFactorCondition>> conditionsByStopFactorId = findConditions(stopFactors);

        StopFactorStatus globalStopFactorStatus = APPROVED;
        InternalStatusCode internalStatusCode = null;
        List<StopFactorCheck> stopFactorChecks = new ArrayList<>(stopFactors.size());

        for (StopFactor s : stopFactors) {
            List<StopFactorCondition> conditions = conditionsByStopFactorId.getOrDefault(s.getId(), List.of());
            StopFactorCheck stopFactorCheck = checkStopFactor(scoringId, s, conditions, request, individualReport);
            stopFactorCheck.setProductId(productId);
            stopFactorChecks.add(stopFactorCheck);

            if (stopFactorCheck.getResult() == REJECTED) {
                globalStopFactorStatus = REJECTED;
                if (isCurrentOverdue(s)) internalStatusCode = InternalStatusCode._451;
            } else if (globalStopFactorStatus != REJECTED && stopFactorCheck.getResult() == UNDERWRITER) {
                globalStopFactorStatus = UNDERWRITER;
            }
        }

        // A repeated check of the same scoring replaces its previous results instead of duplicating them
        stopFactorCheckRepository.deleteByScoringId(scoringId);
        stopFactorCheckRepository.saveAll(stopFactorChecks);

        return new StopFactorDecision(globalStopFactorStatus, internalStatusCode);
    }

    /**
     * Works for scorings checked before and after migration 004: old check rows point to version 1 (the original
     * id), new rows to the version that was current; stopFactorId is always the original id.
     */
    @Override
    @Transactional(readOnly = true)
    public List<StopFactorProjection> findByScoringId(Long scoringId) {
        return stopFactorCheckRepository.findByScoringIdForProjection(scoringId);
    }

    /**
     * Legacy saved {@code new StopFactor(name, description)}. After migration 004 a stop factor needs a metric, so
     * the metric is found by the name (metric title or name template, e.g. "Количество активных споров КИБ").
     * Metrics with params (periods, sources) need the console: POST /api/v1/admin/stop-factors.
     */
    @Override
    @Transactional
    public void save(String name, String description) {
        StopFactorMetric metric = stopFactorMetricRepository.findFirstByTitleOrNameTemplateOrderById(name, name)
                .orElseThrow(() -> new BusinessValidationException("Метрика для стоп-фактора '" + name
                        + "' не найдена, создайте стоп-фактор в консоли с метрикой и параметрами"));
        stopFactorConsoleService.create(new StopFactorRequest(
                metric.getId(), name, description, null, true, Map.of(), Set.of(), List.of(), null), SPR_USER);
    }

    /**
     * Legacy saved the condition directly to stop factor dto.stopFactorId(). After migration 004 conditions belong
     * to a version, so the change creates a new version (the old version keeps its conditions, so checks already
     * made with it still show the conditions they were made with). A condition with the same result
     * (REJECTED / UNDERWRITER) is replaced, the other one is kept.
     *
     * @param dto stopFactorId - id of the stop factor (its first version, ids 1..34 for the legacy ones)
     */
    @Override
    @Transactional
    public void saveCondition(StopFactorConditionDto dto) {
        StopFactorResponse current = stopFactorConsoleService.get(dto.stopFactorId());
        List<ConditionDto> conditions = new ArrayList<>(current.conditions().stream()
                .filter(c -> c.result() != dto.result())
                .toList());
        conditions.add(new ConditionDto(null, dto.result(), dto.valueType(), dto.value()));

        stopFactorConsoleService.update(dto.stopFactorId(), new StopFactorRequest(
                current.metricId(),
                current.name(),
                current.description(),
                current.orderNumber(),
                current.active(),
                current.params(),
                current.productIds(),
                conditions,
                "Условие " + dto.result() + " изменено через StopFactorService.saveCondition"
        ), SPR_USER);
    }

    // ================================================================================================
    // Check of one application
    // ================================================================================================

    /**
     * Legacy stopFactorRepository.findAllActive(): active current versions with the metric, ordered by order_number.
     * A stop factor without products applies to every product; a scoring without product gets all of them.
     */
    private List<StopFactor> findAllActive(Long productId) {
        return stopFactorRepository.findCurrentActiveWithMetric().stream()
                .filter(s -> productId == null || s.getProductIds().isEmpty() || s.getProductIds().contains(productId))
                .toList();
    }

    // Conditions of all checked stop factors in one query (legacy queried every stop factor and result separately)
    private Map<Long, List<StopFactorCondition>> findConditions(List<StopFactor> stopFactors) {
        if (stopFactors.isEmpty()) {
            return Map.of();
        }
        return stopFactorConditionRepository.findAllByStopFactor_IdIn(stopFactors.stream().map(StopFactor::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(c -> c.getStopFactor().getId()));
    }

    // Result of one stop factor: its REJECTED condition first, the UNDERWRITER condition only if not rejected
    private StopFactorCheck checkStopFactor(long scoringId, StopFactor s, List<StopFactorCondition> conditions,
                                            StopFactorCheckRequest request, IndividualReport individualReport) {
        OptionalDouble variable = getVariableFunction(s, request);
        if (variable.isEmpty()) {
            return getNoDataStopFactorCheck(scoringId, s);
        }
        double count = variable.getAsDouble();

        StopFactorCheck stopFactorCheck = new StopFactorCheck(scoringId, s, String.valueOf(count));
        stopFactorCheck = getStopFactorCheck(scoringId, individualReport, s, conditions, stopFactorCheck, count, REJECTED);
        if (stopFactorCheck.getResult() != REJECTED) {
            stopFactorCheck = getStopFactorCheck(scoringId, individualReport, s, conditions, stopFactorCheck, count, UNDERWRITER);
        }
        return stopFactorCheck;
    }

    /**
     * Value of the stop factor, calculated by its metric with its params (migration 004-backfill-stop-factor).
     * Legacy used a switch over ids, which knows only ids 1..34:
     * <pre>
     * 1                       -> KIB_REFUSAL_STATUSES      (count 0, the statuses are checked by the ARRAY condition)
     * 2, 3                    -> KIB_ACTIVE_DISPUTES, KIB_UNJUSTIFIED_DISPUTES
     * 4, 5, 6, 7              -> RESTRUCTURING_COUNT       {source KIB/OB, months 1-12 / 13-24}
     * 8                       -> BUREAU_REQUESTS_COUNT     {days 7}
     * 9, 10                   -> CURRENT_OVERDUE_DAYS, CURRENT_OVERDUE_AMOUNT
     * 11                      -> NEW_CREDITS_COUNT         {days 30}
     * 12                      -> OPEN_CREDITS_COUNT
     * 17, 18, 20..34          -> OVERDUE_BUCKET_COUNT      {months from/to, overdue days min/max}
     * 13, 14, 15, 16, 19      -> OVERDUE_BUCKET_COUNT      inactive, periods without a value -> NO_DATA
     * </pre>
     *
     * @return empty if the reports have no value for these params
     */
    private OptionalDouble getVariableFunction(StopFactor s, StopFactorCheckRequest request) {
        Optional<CheckedValue> value = calculatorRegistry.get(s.getMetric().getCalculator()).calculate(s.getParams(), request);
        if (value.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(value.get().isNumber() ? value.get().number() : 0);
    }

    // A stop factor that cannot be checked must not silently pass the application
    private static StopFactorCheck getNoDataStopFactorCheck(long scoringId, StopFactor s) {
        log.warn("Скоринг {}: нет данных для стоп-фактора {} '{}' ({} {}), заявка направлена андеррайтеру",
                scoringId, s.getFactorId(), s.getName(), s.getMetric().getCalculator(), s.getParams());
        StopFactorCheck stopFactorCheck = new StopFactorCheck(scoringId, s, NO_DATA);
        stopFactorCheck.setResult(UNDERWRITER);
        return stopFactorCheck;
    }

    private static boolean isCurrentOverdue(StopFactor s) {
        return CURRENT_OVERDUE_CALCULATORS.contains(s.getMetric().getCalculator());
    }

    // ================================================================================================
    // Conditions
    // ================================================================================================

    /**
     * Checks the condition of the stop factor with result expectedStatus (REJECTED or UNDERWRITER).
     * No such condition - the check is returned unchanged.
     */
    private static StopFactorCheck getStopFactorCheck(long scoringId, IndividualReport individualReport, StopFactor s,
                                                      List<StopFactorCondition> conditions, StopFactorCheck stopFactorCheck,
                                                      double count, StopFactorStatus expectedStatus) {
        Optional<StopFactorCondition> optionalCondition = conditions.stream()
                .filter(c -> c.getResult() == expectedStatus)
                .findFirst();
        if (optionalCondition.isEmpty()) {
            return stopFactorCheck;
        }
        StopFactorCondition condition = optionalCondition.get();

        return switch (condition.getValueType()) {
            // STRING is a one-status list, the console allows it for status stop factors
            case ARRAY, STRING -> {
                List<LoanStatus> loanStatusList = getLoanStatuses(condition.getValue().split(","));
                StopFactorResult result = checkLoanNotValidStatuses(individualReport, expectedStatus, loanStatusList);
                StopFactorCheck statusCheck = new StopFactorCheck(scoringId, s, result.getValue());
                statusCheck.setResult(result.getStatus());
                yield statusCheck;
            }
            case INTEGER -> getNumberStopFactorCheck(scoringId, s, count, Integer.parseInt(condition.getValue()), expectedStatus);
            case DOUBLE -> getNumberStopFactorCheck(scoringId, s, count, Double.parseDouble(condition.getValue()), expectedStatus);
        };
    }

    // A number condition holds the highest allowed value and fires when the value is greater
    private static StopFactorCheck getNumberStopFactorCheck(long scoringId, StopFactor s, double count,
                                                            double conditionValue, StopFactorStatus expectedStatus) {
        StopFactorCheck stopFactorCheck = new StopFactorCheck(scoringId, s, String.valueOf(count));
        if (count > conditionValue) {
            stopFactorCheck.setResult(expectedStatus);
        }
        return stopFactorCheck;
    }

    // ================================================================================================
    // Loan statuses
    // ================================================================================================

    // A status unknown to LoanStatus is skipped with a warning; legacy LoanStatus.valueOf failed the whole check
    private static @NotNull List<LoanStatus> getLoanStatuses(String[] array) {
        List<LoanStatus> loanStatusList = new ArrayList<>();
        for (String a : array) {
            String name = a.trim();
            if (name.isEmpty()) {
                continue;
            }
            try {
                loanStatusList.add(LoanStatus.valueOf(name));
            } catch (IllegalArgumentException e) {
                log.warn("Неизвестный статус кредита '{}' в условии стоп-фактора, пропущен", name);
            }
        }
        return loanStatusList;
    }

    /**
     * @return expectedStatus and the status of the first loan of the client (main debtor or guarantor) with one of
     * the condition statuses, or APPROVED and "Нет совпадений"
     */
    private static StopFactorResult checkLoanNotValidStatuses(IndividualReport individualReport, StopFactorStatus expectedStatus,
                                                              List<LoanStatus> loanStatusList) {
        if (individualReport != null) {
            for (LoanDto loan : individualReport.loanDtoList()) {
                if (StopFactorValuesInitializer.isMainDebtorOrGuarantor(loan) && loanStatusList.contains(loan.getLoanStatus())) {
                    return new StopFactorResult(expectedStatus, String.valueOf(loan.getLoanStatus()));
                }
            }
        }
        return new StopFactorResult(APPROVED, NO_MATCHES);
    }
}
