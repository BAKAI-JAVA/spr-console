package kg.bakai.stopfactor.service.impl;

import kg.bakai.stopfactor.dto.*;
import kg.bakai.stopfactor.entity.StopFactor;
import kg.bakai.stopfactor.entity.StopFactorCheck;
import kg.bakai.stopfactor.entity.StopFactorCondition;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.repository.StopFactorCheckRepository;
import kg.bakai.stopfactor.repository.StopFactorConditionRepository;
import kg.bakai.stopfactor.repository.StopFactorRepository;
import kg.bakai.stopfactor.service.StopFactorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StopFactorServiceImpl implements StopFactorService {

    /// Stop factors that mean "current overdue" and produce a dedicated rejection code
    private static final Set<Long> CURRENT_OVERDUE_STOP_FACTOR_IDS = Set.of(9L, 10L);

    private final StopFactorRepository stopFactorRepository;
    private final StopFactorConditionRepository stopFactorConditionRepository;
    private final StopFactorCheckRepository stopFactorCheckRepository;

    @Override
    @Transactional
    public StopFactorCheckResponse check(StopFactorCheckRequest request) {
        long scoringId = request.scoringId();
        List<StopFactor> stopFactors = stopFactorRepository.findAllActive();
        Map<Long, List<StopFactorCondition>> conditionsByStopFactorId = stopFactorConditionRepository
                .findAllByStopFactor_IdIn(stopFactors.stream().map(StopFactor::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(c -> c.getStopFactor().getId()));

        // Re-running a check for the same scoring replaces the previous results
        stopFactorCheckRepository.deleteByScoringId(scoringId);

        List<StopFactorCheck> checks = new ArrayList<>();
        List<StopFactorResult> results = new ArrayList<>();
        for (StopFactor stopFactor : stopFactors) {
            Optional<CheckedValue> checkedValue = extractValue(stopFactor.getId(), request);
            if (checkedValue.isEmpty()) {
                log.warn("Stop factor {} '{}' has no value mapping, skipped", stopFactor.getId(), stopFactor.getName());
                continue;
            }

            StopFactorStatus status = evaluate(
                    checkedValue.get(),
                    conditionsByStopFactorId.getOrDefault(stopFactor.getId(), List.of())
            );

            StopFactorCheck check = new StopFactorCheck(scoringId, stopFactor, checkedValue.get().asString());
            check.setResult(status);
            checks.add(check);
            results.add(new StopFactorResult(status, stopFactor.getName(), checkedValue.get().asString()));
        }
        stopFactorCheckRepository.saveAll(checks);

        StopFactorDecision decision = decide(checks);
        log.info("Stop factor check for scoring {} finished with {} ({})",
                scoringId, decision.status(), decision.internalStatusCode().getCode());
        return new StopFactorCheckResponse(scoringId, decision, results);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopFactorDto> getActiveStopFactors() {
        return stopFactorRepository.findAllActive().stream()
                .map(sf -> new StopFactorDto(sf.getId(), sf.getName(), sf.getOrderNumber()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopFactorConditionDto> getConditions(long stopFactorId) {
        return stopFactorConditionRepository.findAllByStopFactor_Id(stopFactorId).stream()
                .map(c -> new StopFactorConditionDto(stopFactorId, c.getValue(), c.getValueType(), c.getResult()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopFactorCheckDto> getChecks(long scoringId) {
        return stopFactorCheckRepository.findByScoringIdForProjection(scoringId).stream()
                .map(p -> new StopFactorCheckDto(
                        p.getStopFactorId(),
                        p.getStopFactorName(),
                        p.getCheckResult(),
                        p.getCheckedValue(),
                        p.getRejectedValue(),
                        p.getRejectedValueType(),
                        p.getUnderwriterValue(),
                        p.getUnderwriterValueType()
                ))
                .toList();
    }

    /// REJECTED has priority over UNDERWRITER, which has priority over APPROVED
    private StopFactorStatus evaluate(CheckedValue value, List<StopFactorCondition> conditions) {
        if (matchesAny(value, conditions, StopFactorStatus.REJECTED)) {
            return StopFactorStatus.REJECTED;
        }
        if (matchesAny(value, conditions, StopFactorStatus.UNDERWRITER)) {
            return StopFactorStatus.UNDERWRITER;
        }
        return StopFactorStatus.APPROVED;
    }

    private boolean matchesAny(CheckedValue value, List<StopFactorCondition> conditions, StopFactorStatus result) {
        return conditions.stream()
                .filter(c -> c.getResult() == result)
                .anyMatch(c -> matches(value, c));
    }

    /// Numeric conditions hold the maximum allowed value: the condition triggers when the checked value exceeds it.
    /// ARRAY/STRING conditions trigger when the checked value contains one of the listed values.
    private boolean matches(CheckedValue value, StopFactorCondition condition) {
        return switch (condition.getValueType()) {
            case INTEGER, DOUBLE -> value.number() != null
                    && value.number() > Double.parseDouble(condition.getValue().trim());
            case ARRAY, STRING -> {
                Set<String> conditionValues = Arrays.stream(condition.getValue().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());
                yield value.values().stream()
                        .map(s -> s.trim().toLowerCase(Locale.ROOT))
                        .anyMatch(conditionValues::contains);
            }
        };
    }

    private StopFactorDecision decide(List<StopFactorCheck> checks) {
        List<StopFactorCheck> rejected = checks.stream()
                .filter(c -> c.getResult() == StopFactorStatus.REJECTED)
                .toList();
        if (!rejected.isEmpty()) {
            boolean onlyCurrentOverdue = rejected.stream()
                    .allMatch(c -> CURRENT_OVERDUE_STOP_FACTOR_IDS.contains(c.getStopFactor().getId()));
            return new StopFactorDecision(
                    StopFactorStatus.REJECTED,
                    onlyCurrentOverdue ? InternalStatusCode._451 : InternalStatusCode._450
            );
        }
        boolean underwriter = checks.stream().anyMatch(c -> c.getResult() == StopFactorStatus.UNDERWRITER);
        if (underwriter) {
            return new StopFactorDecision(StopFactorStatus.UNDERWRITER, InternalStatusCode._301);
        }
        return new StopFactorDecision(StopFactorStatus.APPROVED, InternalStatusCode._200);
    }

    /// Maps stop factor ids from stop_factors.stop_factor to the request values
    private Optional<CheckedValue> extractValue(long stopFactorId, StopFactorCheckRequest request) {
        if (stopFactorId == 1) {
            List<String> statuses = request.creditHistoryStatuses() != null ? request.creditHistoryStatuses() : List.of();
            return Optional.of(CheckedValue.ofValues(statuses));
        }
        StopFactorIntegerValueDto v = request.values();
        OverdueForStopFactor o = v.overdue() != null ? v.overdue() : new StopFactorIntegerValueDto(0, 0).overdue();
        double number = switch ((int) stopFactorId) {
            case 2 -> v.numberOfActiveDisputes();
            case 3 -> v.numberOfUnjustifiedDisputesInPast();
            case 4 -> v.numberOfRestructuringsLast12Months();
            case 5 -> v.countOfChangesObLast12Month();
            case 6 -> v.numberOfRestructuringsFrom12To24Months();
            case 7 -> v.countOfChangesObFrom13To24Months();
            case 8 -> v.creditInquiriesLast7Days();
            case 9 -> v.currentNumberOfDaysOverdue();
            case 10 -> v.currentAmountOfOverdue();
            case 11 -> v.totalNumberOfNewOpenLoansInTheLast30Days();
            case 12 -> v.totalOpenCreditContracts();
            // 1-3 months
            case 27 -> o.countWherePeriodFrom1To3MonthsAndOverdueFrom1To15();
            case 28 -> o.countWherePeriodFrom1To3MonthsAndOverdueFrom16To30();
            case 29 -> o.countWherePeriodFrom1To3MonthsAndOverdueFrom31();
            // 4-6 months
            case 30 -> o.countWherePeriodFrom4To6MonthsAndOverdueFrom1To15();
            case 31 -> o.countWherePeriodFrom4To6MonthsAndOverdueFrom16To30();
            case 32 -> o.countWherePeriodFrom4To6MonthsAndOverdueFrom31();
            // 7-12 months
            case 17 -> o.countWherePeriodFrom7To12MonthsAndOverdueFrom1To10();
            case 18 -> o.countWherePeriodFrom7To12MonthsAndOverdueFrom11To20();
            case 33 -> o.countWherePeriodFrom7To12MonthsAndOverdueFrom21To30();
            case 34 -> o.countWherePeriodFrom7To12MonthsAndOverdueFrom31();
            // 13-24 months
            case 20 -> o.countWherePeriodFrom13To24MonthsAndOverdueFrom1To10();
            case 21 -> o.countWherePeriodFrom13To24MonthsAndOverdueFrom11To20();
            case 22 -> o.countWherePeriodFrom13To24MonthsAndOverdueFrom21To30();
            case 23 -> o.countWherePeriodFrom13To24MonthsAndOverdueFrom31();
            // 25-36 months
            case 24 -> o.countWherePeriodFrom25To36MonthsAndOverdueFrom1To60();
            case 25 -> o.countWherePeriodFrom25To36MonthsAndOverdueFrom61To90();
            case 26 -> o.countWherePeriodFrom25To36MonthsAndOverdueFrom91();
            default -> {
                log.debug("No mapping for stop factor {}", stopFactorId);
                yield Double.NaN;
            }
        };
        if (Double.isNaN(number)) {
            return Optional.empty();
        }
        return Optional.of(CheckedValue.ofNumber(number));
    }

    private record CheckedValue(Double number, List<String> values) {
        static CheckedValue ofNumber(double number) {
            return new CheckedValue(number, List.of());
        }

        static CheckedValue ofValues(List<String> values) {
            return new CheckedValue(null, values);
        }

        String asString() {
            if (number != null) {
                return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
            }
            return String.join(", ", values);
        }
    }
}
