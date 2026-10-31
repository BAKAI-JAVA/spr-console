package kg.bakai.stopfactor.service.impl;

import kg.bakai.stopfactor.enums.InternalStatusCode;
import kg.bakai.stopfactor.calculator.CalculatorRegistry;
import kg.bakai.stopfactor.calculator.CheckedValue;
import kg.bakai.stopfactor.dto.scoring.*;
import kg.bakai.stopfactor.entity.Product;
import kg.bakai.stopfactor.entity.StopFactor;
import kg.bakai.stopfactor.entity.StopFactorCheck;
import kg.bakai.stopfactor.entity.StopFactorCondition;
import kg.bakai.stopfactor.enums.CalculatorType;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.evaluation.ConditionMatcher;
import kg.bakai.stopfactor.exception.BusinessValidationException;
import kg.bakai.stopfactor.repository.ProductRepository;
import kg.bakai.stopfactor.repository.StopFactorCheckRepository;
import kg.bakai.stopfactor.repository.StopFactorConditionRepository;
import kg.bakai.stopfactor.repository.StopFactorRepository;
import kg.bakai.stopfactor.service.StopFactorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StopFactorServiceImpl implements StopFactorService {

    static final String NO_DATA = "NO_DATA";

    private static final Set<CalculatorType> CURRENT_OVERDUE_CALCULATORS =
            EnumSet.of(CalculatorType.CURRENT_OVERDUE_DAYS, CalculatorType.CURRENT_OVERDUE_AMOUNT);

    private final StopFactorRepository stopFactorRepository;
    private final StopFactorConditionRepository stopFactorConditionRepository;
    private final StopFactorCheckRepository stopFactorCheckRepository;
    private final ProductRepository productRepository;
    private final CalculatorRegistry calculatorRegistry;
    private final ConditionMatcher conditionMatcher;

    @Override
    @Transactional
    public StopFactorCheckResponse check(StopFactorCheckRequest request) {
        long scoringId = request.scoringId();
        Product product = request.productId() != null
                ? productRepository.findById(request.productId())
                        .orElseThrow(() -> new BusinessValidationException("productId: продукт " + request.productId() + " не существует"))
                : null;

        List<StopFactor> stopFactors = stopFactorRepository.findCurrentActiveWithMetric().stream()
                .filter(sf -> appliesTo(sf, product))
                .toList();
        Map<Long, List<StopFactorCondition>> conditionsByVersionId = stopFactorConditionRepository
                .findAllByStopFactor_IdIn(stopFactors.stream().map(StopFactor::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(c -> c.getStopFactor().getId()));

        stopFactorCheckRepository.deleteByScoringId(scoringId);

        List<StopFactorCheck> checks = new ArrayList<>();
        List<StopFactorResult> results = new ArrayList<>();
        for (StopFactor stopFactor : stopFactors) {
            Optional<CheckedValue> checkedValue = calculatorRegistry.get(stopFactor.getMetric().getCalculator())
                    .calculate(stopFactor.getParams(), request);

            StopFactorStatus status;
            String value;
            if (checkedValue.isPresent()) {
                status = conditionMatcher.evaluate(
                        checkedValue.get(),
                        conditionsByVersionId.getOrDefault(stopFactor.getId(), List.of())
                );
                value = checkedValue.get().asString();
            } else {
                // Стоп-фактор, который нельзя рассчитать, не должен пропускать заявку
                log.warn("Скоринг {}: нет данных для стоп-фактора {} '{}' ({} {}), заявка направлена андеррайтеру",
                        scoringId, stopFactor.getFactorId(), stopFactor.getName(),
                        stopFactor.getMetric().getCalculator(), stopFactor.getParams());
                status = StopFactorStatus.UNDERWRITER;
                value = NO_DATA;
            }

            StopFactorCheck check = new StopFactorCheck(scoringId, stopFactor, value);
            check.setResult(status);
            check.setProduct(product);
            checks.add(check);
            results.add(new StopFactorResult(status, stopFactor.getName(), value));
        }
        stopFactorCheckRepository.saveAll(checks);

        StopFactorDecision decision = decide(checks);
        log.info("Проверка стоп-факторов для скоринга {} завершена: {} ({})",
                scoringId, decision.status(), decision.internalStatusCode().getCode());
        return new StopFactorCheckResponse(scoringId, decision, results);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopFactorDto> getActiveStopFactors() {
        return stopFactorRepository.findAllActive().stream()
                .map(sf -> new StopFactorDto(sf.getFactorId(), sf.getName(), sf.getOrderNumber()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopFactorConditionDto> getConditions(long stopFactorId) {
        return stopFactorRepository.findCurrent(stopFactorId)
                .map(current -> stopFactorConditionRepository.findAllByStopFactor_Id(current.getId()).stream()
                        .map(c -> new StopFactorConditionDto(stopFactorId, c.getValue(), c.getValueType(), c.getResult()))
                        .toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopFactorCheckDto> getChecks(long scoringId) {
        return stopFactorCheckRepository.findByScoringIdForProjection(scoringId).stream()
                .map(p -> new StopFactorCheckDto(
                        p.getStopFactorId(),
                        p.getStopFactorVersionId(),
                        p.getVersionNumber(),
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

    private boolean appliesTo(StopFactor stopFactor, Product product) {
        return product == null
                || stopFactor.getProductIds().isEmpty()
                || stopFactor.getProductIds().contains(product.getId());
    }

    private StopFactorDecision decide(List<StopFactorCheck> checks) {
        List<StopFactorCheck> rejected = checks.stream()
                .filter(c -> c.getResult() == StopFactorStatus.REJECTED)
                .toList();
        if (!rejected.isEmpty()) {
            boolean onlyCurrentOverdue = rejected.stream()
                    .allMatch(c -> CURRENT_OVERDUE_CALCULATORS.contains(c.getStopFactor().getMetric().getCalculator()));
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
}
