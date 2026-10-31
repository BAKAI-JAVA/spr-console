package kg.bakai.stopfactor.service.admin.impl;

import kg.bakai.stopfactor.dto.admin.ConditionDto;
import kg.bakai.stopfactor.dto.admin.StopFactorRequest;
import kg.bakai.stopfactor.dto.admin.StopFactorResponse;
import kg.bakai.stopfactor.entity.StopFactor;
import kg.bakai.stopfactor.entity.StopFactorCondition;
import kg.bakai.stopfactor.entity.StopFactorMetric;
import kg.bakai.stopfactor.evaluation.NameTemplateRenderer;
import kg.bakai.stopfactor.exception.BusinessValidationException;
import kg.bakai.stopfactor.exception.NotFoundException;
import kg.bakai.stopfactor.repository.StopFactorConditionRepository;
import kg.bakai.stopfactor.repository.StopFactorMetricRepository;
import kg.bakai.stopfactor.repository.StopFactorRepository;
import kg.bakai.stopfactor.service.admin.StopFactorAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StopFactorAdminServiceImpl implements StopFactorAdminService {

    private static final String CREATED_REASON = "Создание стоп-фактора";

    private final StopFactorRepository stopFactorRepository;
    private final StopFactorConditionRepository conditionRepository;
    private final StopFactorMetricRepository metricRepository;
    private final StopFactorDefinitionValidator validator;

    @Override
    @Transactional(readOnly = true)
    public List<StopFactorResponse> getAll(Boolean active, Long metricId, Long productId) {
        List<StopFactor> stopFactors = stopFactorRepository.findAllCurrentWithMetric().stream()
                .filter(sf -> active == null || sf.isActive() == active)
                .filter(sf -> metricId == null || sf.getMetric().getId().equals(metricId))
                .filter(sf -> productId == null || sf.getProductIds().isEmpty() || sf.getProductIds().contains(productId))
                .toList();
        return toResponses(stopFactors);
    }

    @Override
    @Transactional(readOnly = true)
    public StopFactorResponse get(long factorId) {
        return toResponses(List.of(findCurrent(factorId))).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopFactorResponse> getVersions(long factorId) {
        List<StopFactor> versions = stopFactorRepository.findVersions(factorId);
        if (versions.isEmpty()) {
            throw new NotFoundException("Стоп-фактор " + factorId + " не найден");
        }
        return toResponses(versions);
    }

    @Override
    @Transactional
    public StopFactorResponse create(StopFactorRequest request, String user) {
        StopFactorMetric metric = metricRepository.findById(request.metricId())
                .orElseThrow(() -> new BusinessValidationException("metricId: метрика " + request.metricId() + " не существует"));

        List<String> errors = new ArrayList<>();
        if (!metric.isActive()) {
            errors.add("metricId: метрика '" + metric.getTitle() + "' неактивна");
        }
        Map<String, Object> params = validator.validateParams(metric, request.params(), errors);
        List<ConditionDto> conditions = validator.validateConditions(metric, request.conditions(), errors);
        Set<Long> productIds = request.productIds() != null ? request.productIds() : Set.of();
        validator.validateProducts(productIds, errors);
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }

        StopFactor stopFactor = new StopFactor();
        stopFactor.setMetric(metric);
        stopFactor.setParams(new HashMap<>(params));
        stopFactor.setName(resolveName(request.name(), metric, params));
        stopFactor.setDescription(blankToNull(request.description()));
        stopFactor.setActive(request.active() == null || request.active());
        stopFactor.setOrderNumber(request.orderNumber() != null
                ? request.orderNumber()
                : stopFactorRepository.findMaxCurrentOrderNumber() + 1);
        stopFactor.setProductIds(new HashSet<>(productIds));
        stopFactor.setValidFrom(LocalDateTime.now());
        stopFactor.setChangeReason(request.changeReason() != null && !request.changeReason().isBlank()
                ? request.changeReason().trim()
                : CREATED_REASON);
        stopFactor.setCreatedBy(user);

        StopFactor saved = stopFactorRepository.save(stopFactor);
        saveConditions(saved, conditions);
        log.info("Стоп-фактор {} '{}' создан пользователем {}", saved.getFactorId(), saved.getName(), user);
        return get(saved.getFactorId());
    }

    @Override
    @Transactional
    public StopFactorResponse update(long factorId, StopFactorRequest request, String user) {
        StopFactor current = lockAndFindCurrent(factorId);
        StopFactorMetric metric = current.getMetric();
        int orderNumber = request.orderNumber() != null ? request.orderNumber() : current.getOrderNumber();
        boolean active = request.active() != null ? request.active() : current.isActive();

        List<String> errors = new ArrayList<>();
        if (!metric.getId().equals(request.metricId())) {
            errors.add("metricId: метрику стоп-фактора нельзя изменить, создайте новый стоп-фактор");
        }
        if (request.changeReason() == null || request.changeReason().isBlank()) {
            errors.add("changeReason: причина изменения обязательна");
        }
        if (active && !current.isActive() && !metric.isActive()) {
            errors.add("active: метрика '" + metric.getTitle() + "' неактивна, активируйте ее перед активацией стоп-фактора");
        }
        Map<String, Object> params = validator.validateParams(metric, request.params(), errors);
        List<ConditionDto> conditions = validator.validateConditions(metric, request.conditions(), errors);
        Set<Long> productIds = request.productIds() != null ? request.productIds() : Set.of();
        validator.validateProducts(productIds, errors);
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }

        boolean paramsChanged = !params.equals(current.getParams());
        String name = request.name() != null && !request.name().isBlank()
                ? request.name().trim()
                : paramsChanged ? NameTemplateRenderer.render(metric.getNameTemplate(), params) : current.getName();
        String description = blankToNull(request.description());
        List<StopFactorCondition> currentConditions = conditionRepository.findAllByStopFactor_Id(current.getId());

        boolean unchanged = !paramsChanged
                && name.equals(current.getName())
                && Objects.equals(description, current.getDescription())
                && orderNumber == current.getOrderNumber()
                && active == current.isActive()
                && productIds.equals(current.getProductIds())
                && conditionKeys(conditions).equals(conditionKeysOf(currentConditions));
        if (unchanged) {
            log.info("Стоп-фактор {}: изменений от {} нет, новая версия не создана", factorId, user);
            return get(factorId);
        }

        StopFactor next = closeAndCreateNextVersion(current, request.changeReason(), user);
        next.setParams(new HashMap<>(params));
        next.setName(name);
        next.setDescription(description);
        next.setOrderNumber(orderNumber);
        next.setActive(active);
        next.setProductIds(new HashSet<>(productIds));
        StopFactor saved = stopFactorRepository.save(next);
        saveConditions(saved, conditions);
        log.info("Стоп-фактор {} обновлен до версии {} пользователем {}: {}", factorId, saved.getVersionNumber(), user, request.changeReason());
        return get(factorId);
    }

    @Override
    @Transactional
    public StopFactorResponse changeStatus(long factorId, boolean active, String changeReason, String user) {
        if (changeReason == null || changeReason.isBlank()) {
            throw new BusinessValidationException("changeReason: причина изменения обязательна");
        }
        StopFactor current = lockAndFindCurrent(factorId);
        if (current.isActive() == active) {
            return get(factorId);
        }
        if (active && !current.getMetric().isActive()) {
            throw new BusinessValidationException("Метрика '" + current.getMetric().getTitle()
                    + "' неактивна, активируйте ее перед активацией стоп-фактора");
        }

        List<StopFactorCondition> conditions = conditionRepository.findAllByStopFactor_Id(current.getId());
        StopFactor next = closeAndCreateNextVersion(current, changeReason, user);
        next.setActive(active);
        StopFactor saved = stopFactorRepository.save(next);
        saveConditions(saved, conditions.stream()
                .map(c -> new ConditionDto(null, c.getResult(), c.getValueType(), c.getValue()))
                .toList());
        log.info("Стоп-фактор {} {} пользователем {} (версия {}): {}", factorId, active ? "активирован" : "деактивирован",
                user, saved.getVersionNumber(), changeReason);
        return get(factorId);
    }

    @Override
    @Transactional
    // Порядок не влияет на решение, поэтому меняется на месте, без новой версии
    public List<StopFactorResponse> reorder(List<Long> factorIds) {
        List<StopFactor> currents = stopFactorRepository.findAllCurrentWithMetric();
        Map<Long, StopFactor> byFactorId = currents.stream()
                .collect(Collectors.toMap(StopFactor::getFactorId, sf -> sf));

        List<String> errors = new ArrayList<>();
        Set<Long> requested = new LinkedHashSet<>();
        for (Long factorId : factorIds) {
            if (factorId == null) {
                errors.add("factorIds: не должен содержать null");
            } else if (!requested.add(factorId)) {
                errors.add("factorIds: " + factorId + " указан больше одного раза");
            } else if (!byFactorId.containsKey(factorId)) {
                errors.add("factorIds: стоп-фактор " + factorId + " не существует");
            }
        }
        byFactorId.keySet().stream()
                .filter(id -> !requested.contains(id))
                .sorted()
                .forEach(id -> errors.add("factorIds: отсутствует стоп-фактор " + id + ", нужно указать все стоп-факторы"));
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }

        int order = 1;
        for (Long factorId : requested) {
            byFactorId.get(factorId).setOrderNumber(order++);
        }
        stopFactorRepository.flush();
        log.info("Порядок стоп-факторов изменен: {}", requested);
        return toResponses(stopFactorRepository.findAllCurrentWithMetric());
    }

    // Текущая версия закрывается и сохраняется (flush) до вставки новой: у стоп-фактора может быть только одна
    // текущая версия (ux_stop_factor_current_version), а Hibernate выполняет INSERT раньше UPDATE
    private StopFactor closeAndCreateNextVersion(StopFactor current, String changeReason, String user) {
        LocalDateTime now = LocalDateTime.now();
        current.setValidTo(now);
        stopFactorRepository.saveAndFlush(current);

        StopFactor next = current.newVersion(now, changeReason.trim());
        next.setCreatedBy(user);
        return next;
    }

    private void saveConditions(StopFactor version, List<ConditionDto> conditions) {
        List<StopFactorCondition> entities = conditions.stream().map(request -> {
            StopFactorCondition condition = new StopFactorCondition();
            condition.setStopFactor(version);
            condition.setResult(request.result());
            condition.setValueType(request.valueType());
            condition.setValue(request.value());
            return condition;
        }).toList();
        conditionRepository.saveAll(entities);
    }

    private StopFactor lockAndFindCurrent(long factorId) {
        stopFactorRepository.lockFactor(factorId)
                .orElseThrow(() -> new NotFoundException("Стоп-фактор " + factorId + " не найден"));
        return findCurrent(factorId);
    }

    private StopFactor findCurrent(long factorId) {
        return stopFactorRepository.findCurrent(factorId)
                .orElseThrow(() -> new NotFoundException("Стоп-фактор " + factorId + " не найден"));
    }

    private String resolveName(String name, StopFactorMetric metric, Map<String, Object> params) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return NameTemplateRenderer.render(metric.getNameTemplate(), params);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Set<String> conditionKeys(List<ConditionDto> conditions) {
        return conditions.stream()
                .map(c -> c.result() + "|" + c.valueType() + "|" + c.value())
                .collect(Collectors.toSet());
    }

    private static Set<String> conditionKeysOf(List<StopFactorCondition> conditions) {
        return conditions.stream()
                .map(c -> c.getResult() + "|" + c.getValueType() + "|" + c.getValue())
                .collect(Collectors.toSet());
    }

    private List<StopFactorResponse> toResponses(List<StopFactor> stopFactors) {
        Map<Long, List<StopFactorCondition>> conditions = conditionRepository
                .findAllByStopFactor_IdIn(stopFactors.stream().map(StopFactor::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(c -> c.getStopFactor().getId()));
        return stopFactors.stream()
                .map(sf -> toResponse(sf, conditions.getOrDefault(sf.getId(), List.of())))
                .toList();
    }

    private StopFactorResponse toResponse(StopFactor sf, List<StopFactorCondition> conditions) {
        StopFactorMetric metric = sf.getMetric();
        return new StopFactorResponse(
                sf.getFactorId(),
                sf.getId(),
                sf.getVersionNumber(),
                sf.isCurrent(),
                sf.getName(),
                sf.getDescription(),
                sf.isActive(),
                sf.getOrderNumber(),
                metric.getId(),
                metric.getTitle(),
                metric.getCalculator(),
                new TreeMap<>(sf.getParams()),
                new TreeSet<>(sf.getProductIds()),
                conditions.stream()
                        .sorted(Comparator.comparing(StopFactorCondition::getResult))
                        .map(c -> new ConditionDto(c.getId(), c.getResult(), c.getValueType(), c.getValue()))
                        .toList(),
                sf.getValidFrom(),
                sf.getValidTo(),
                sf.getChangeReason(),
                sf.getCreatedBy(),
                sf.getCreatedDate()
        );
    }
}
