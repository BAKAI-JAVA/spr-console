package kg.bakai.stopfactor.service.impl;

import kg.bakai.common.enums.ProductSegment;
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
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents;
import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.dto.scoring.StopFactorConditionDto;
import kg.bakai.stopfactor.dto.scoring.StopFactorDecision;
import kg.bakai.stopfactor.dto.scoring.StopFactorIntegerValueDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

import static kg.bakai.stopfactor.enums.StopFactorStatus.APPROVED;
import static kg.bakai.stopfactor.enums.StopFactorStatus.REJECTED;
import static kg.bakai.stopfactor.enums.StopFactorStatus.UNDERWRITER;

/**
 * Stop factor check of SPR, working with the versioned stop factors of changelog 1.3.
 * <p>
 * How an application is checked:
 * <ol>
 *     <li>the KIB / OB reports become values and credit history events ({@link StopFactorValuesInitializer});</li>
 *     <li>the stop factors that apply to the scoring's product are taken: active current versions of its segment
 *     or of every segment, limited to the product or to no product ({@link StopFactor#appliesTo});</li>
 *     <li>for each one its metric calculates the value, then its REJECTED condition is applied, then the
 *     UNDERWRITER condition if it did not reject;</li>
 *     <li>any REJECTED rejects the application (451 if a current overdue stop factor rejected), otherwise any
 *     UNDERWRITER sends it to the underwriter; the results replace earlier ones of the scoring.</li>
 * </ol>
 * Differences from the legacy algorithm, which switched over stop factor ids 1..34:
 * <ul>
 *     <li>the value comes from the stop factor's metric and params, so console edits (new versions, new ids) work;</li>
 *     <li>451 is chosen by the metric (current overdue days / amount), not by ids 9 and 10;</li>
 *     <li>a stop factor whose value cannot be calculated goes to the underwriter as {@link #NO_DATA};</li>
 *     <li>overdue history is counted from the episodes SPR builds (KibService.getOverdueEpisodes), so any period
 *     and overdue days range works; the 17 legacy counters are used only when SPR returns no episodes.</li>
 * </ul>
 * Stop factors 1..34 as migrated give exactly the legacy results (LegacyStopFactorParityTest).
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

    /** Checked value of a loan status stop factor when no loan of the client has one of its statuses. */
    static final String NO_MATCHES = "Нет совпадений";

    /** Author of the versions created through {@link #save} and {@link #saveCondition}. */
    static final String SPR_USER = "spr";

    /** Conditions are applied in this order: an UNDERWRITER condition only matters if REJECTED did not fire. */
    private static final List<StopFactorStatus> CONDITION_ORDER = List.of(REJECTED, UNDERWRITER);

    /** A rejection by these metrics (legacy stop factors 9 and 10) gives code 451. */
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
        StopFactorIntegerValueDto values = stopFactorValuesInitializer.values(
                customReport, individualReport, consolidationDto, totalDebt, requestDate);
        CreditHistoryEvents history = stopFactorValuesInitializer.history(
                customReport, individualReport, consolidationDto, requestDate);
        Optional<ProductProjection> product = stopFactorRepository.findProductByScoringId(scoringId);
        Long productId = product.map(ProductProjection::getId).orElse(null);
        ProductSegment segment = product.map(ProductProjection::getSegment).orElse(ProductSegment.DEFAULT);
        StopFactorCheckRequest request = new StopFactorCheckRequest(scoringId, productId, values, history);

        List<StopFactor> stopFactors = findApplicable(productId, segment);
        Map<Long, List<StopFactorCondition>> conditionsByStopFactorId = findConditions(stopFactors);

        List<StopFactorCheck> checks = new ArrayList<>(stopFactors.size());
        for (StopFactor stopFactor : stopFactors) {
            StopFactorCheck check = checkStopFactor(stopFactor,
                    conditionsByStopFactorId.getOrDefault(stopFactor.getId(), List.of()), request, individualReport);
            check.setProductId(productId);
            checks.add(check);
        }

        // A repeated check of the same scoring replaces its previous results instead of duplicating them
        stopFactorCheckRepository.deleteByScoringId(scoringId);
        stopFactorCheckRepository.saveAll(checks);
        return decide(checks);
    }

    /**
     * Works for scorings checked before and after changelog 1.3: old check rows point to version 1 (the original
     * id), new rows to the version that was current; stopFactorId is always the original id.
     */
    @Override
    @Transactional(readOnly = true)
    public List<StopFactorProjection> findByScoringId(Long scoringId) {
        return stopFactorCheckRepository.findByScoringIdForProjection(scoringId);
    }

    /**
     * Legacy saved {@code new StopFactor(name, description)}. Now a stop factor needs a metric, so the metric is
     * found by the name (its title or name template, e.g. "Количество активных споров КИБ"). Metrics with params
     * (periods, sources) need the console: POST /api/v1/admin/stop-factors.
     */
    @Override
    @Transactional
    public void save(String name, String description) {
        StopFactorMetric metric = stopFactorMetricRepository.findFirstByTitleOrNameTemplateOrderById(name, name)
                .orElseThrow(() -> new BusinessValidationException("Метрика для стоп-фактора '" + name
                        + "' не найдена, создайте стоп-фактор в консоли с метрикой и параметрами"));
        stopFactorConsoleService.create(new StopFactorRequest(
                metric.getId(), name, description, null, true, Map.of(), Set.of(), null, List.of(), null), SPR_USER);
    }

    /**
     * Legacy saved the condition directly to the stop factor. Now conditions belong to a version, so the change
     * creates a new version: the old version keeps its conditions, and checks made with it still show them.
     * A condition with the same result (REJECTED / UNDERWRITER) is replaced, the other one is kept.
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
                current.segment(),
                conditions,
                "Условие " + dto.result() + " изменено через StopFactorService.saveCondition"
        ), SPR_USER);
    }

    // ================================================================================================
    // Check of one application
    // ================================================================================================

    /** Active current versions with their metric, in display order, that check applications of this product. */
    private List<StopFactor> findApplicable(Long productId, ProductSegment segment) {
        return stopFactorRepository.findCurrentActiveWithMetric().stream()
                .filter(stopFactor -> stopFactor.appliesTo(productId, segment))
                .toList();
    }

    /** Conditions of all checked stop factors in one query. */
    private Map<Long, List<StopFactorCondition>> findConditions(List<StopFactor> stopFactors) {
        if (stopFactors.isEmpty()) {
            return Map.of();
        }
        return stopFactorConditionRepository.findAllByStopFactor_IdIn(stopFactors.stream().map(StopFactor::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(condition -> condition.getStopFactor().getId()));
    }

    /** Any REJECTED rejects (451 if a current overdue stop factor rejected), otherwise any UNDERWRITER. */
    private static StopFactorDecision decide(List<StopFactorCheck> checks) {
        boolean rejected = checks.stream().anyMatch(check -> check.getResult() == REJECTED);
        if (rejected) {
            boolean currentOverdue = checks.stream()
                    .anyMatch(check -> check.getResult() == REJECTED && isCurrentOverdue(check.getStopFactor()));
            return new StopFactorDecision(REJECTED, currentOverdue ? InternalStatusCode._451 : null);
        }
        boolean underwriter = checks.stream().anyMatch(check -> check.getResult() == UNDERWRITER);
        return new StopFactorDecision(underwriter ? UNDERWRITER : APPROVED, null);
    }

    private static boolean isCurrentOverdue(StopFactor stopFactor) {
        return CURRENT_OVERDUE_CALCULATORS.contains(stopFactor.getMetric().getCalculator());
    }

    /** Result of one stop factor: its REJECTED condition first, the UNDERWRITER condition only if not rejected. */
    private StopFactorCheck checkStopFactor(StopFactor stopFactor, List<StopFactorCondition> conditions,
                                            StopFactorCheckRequest request, IndividualReport individualReport) {
        long scoringId = request.scoringId();
        OptionalDouble value = calculateValue(stopFactor, request);
        if (value.isEmpty()) {
            return noDataCheck(scoringId, stopFactor);
        }

        StopFactorCheck check = new StopFactorCheck(scoringId, stopFactor, String.valueOf(value.getAsDouble()));
        for (StopFactorStatus result : CONDITION_ORDER) {
            if (check.getResult() == REJECTED) {
                break;
            }
            Optional<StopFactorCondition> condition = conditions.stream().filter(c -> c.getResult() == result).findFirst();
            if (condition.isPresent()) {
                check = applyCondition(scoringId, stopFactor, condition.get(), value.getAsDouble(), individualReport);
            }
        }
        return check;
    }

    /**
     * Value of the stop factor calculated by its metric with its params. A loan status metric has no number:
     * its condition is applied to the loans of the KIB report, the value is 0 as in legacy.
     *
     * @return empty if the reports have no value for these params
     */
    private OptionalDouble calculateValue(StopFactor stopFactor, StopFactorCheckRequest request) {
        Optional<CheckedValue> value = calculatorRegistry.get(stopFactor.getMetric().getCalculator())
                .calculate(stopFactor.getParams(), request);
        if (value.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(value.get().isNumber() ? value.get().number() : 0);
    }

    /** A stop factor that cannot be checked must not silently pass the application. */
    private static StopFactorCheck noDataCheck(long scoringId, StopFactor stopFactor) {
        log.warn("Скоринг {}: нет данных для стоп-фактора {} '{}' ({} {}), заявка направлена андеррайтеру",
                scoringId, stopFactor.getFactorId(), stopFactor.getName(), stopFactor.getMetric().getCalculator(),
                stopFactor.getParams());
        StopFactorCheck check = new StopFactorCheck(scoringId, stopFactor, NO_DATA);
        check.setResult(UNDERWRITER);
        return check;
    }

    // ================================================================================================
    // Conditions
    // ================================================================================================

    /**
     * A number condition holds the highest allowed value and fires when the value is greater. A loan status
     * condition (ARRAY, or STRING for one status) fires when a loan of the client has one of its statuses.
     */
    private static StopFactorCheck applyCondition(long scoringId, StopFactor stopFactor, StopFactorCondition condition,
                                                  double value, IndividualReport individualReport) {
        return switch (condition.getValueType()) {
            case ARRAY, STRING -> applyLoanStatusCondition(scoringId, stopFactor, condition, individualReport);
            case INTEGER -> applyNumberCondition(scoringId, stopFactor, condition, value, Integer.parseInt(condition.getValue()));
            case DOUBLE -> applyNumberCondition(scoringId, stopFactor, condition, value, Double.parseDouble(condition.getValue()));
        };
    }

    private static StopFactorCheck applyNumberCondition(long scoringId, StopFactor stopFactor, StopFactorCondition condition,
                                                        double value, double highestAllowed) {
        StopFactorCheck check = new StopFactorCheck(scoringId, stopFactor, String.valueOf(value));
        if (value > highestAllowed) {
            check.setResult(condition.getResult());
        }
        return check;
    }

    /** The checked value is the status found, or {@link #NO_MATCHES}. */
    private static StopFactorCheck applyLoanStatusCondition(long scoringId, StopFactor stopFactor,
                                                            StopFactorCondition condition, IndividualReport individualReport) {
        Optional<LoanStatus> found = findLoanStatus(individualReport, parseLoanStatuses(condition.getValue()));
        StopFactorCheck check = new StopFactorCheck(scoringId, stopFactor, found.map(String::valueOf).orElse(NO_MATCHES));
        check.setResult(found.isPresent() ? condition.getResult() : APPROVED);
        return check;
    }

    /** A status unknown to LoanStatus is skipped with a warning; legacy LoanStatus.valueOf failed the whole check. */
    private static Set<LoanStatus> parseLoanStatuses(String conditionValue) {
        Set<LoanStatus> statuses = EnumSet.noneOf(LoanStatus.class);
        for (String item : conditionValue.split(",")) {
            String name = item.trim();
            if (name.isEmpty()) {
                continue;
            }
            try {
                statuses.add(LoanStatus.valueOf(name));
            } catch (IllegalArgumentException e) {
                log.warn("Неизвестный статус кредита '{}' в условии стоп-фактора, пропущен", name);
            }
        }
        return statuses;
    }

    /** Status of the first loan of the client (main debtor or guarantor) that has one of the statuses. */
    private static Optional<LoanStatus> findLoanStatus(IndividualReport individualReport, Set<LoanStatus> statuses) {
        if (individualReport == null) {
            return Optional.empty();
        }
        return individualReport.loanDtoList().stream()
                .filter(StopFactorValuesInitializer::isMainDebtorOrGuarantor)
                .map(LoanDto::getLoanStatus)
                .filter(statuses::contains)
                .findFirst();
    }
}
