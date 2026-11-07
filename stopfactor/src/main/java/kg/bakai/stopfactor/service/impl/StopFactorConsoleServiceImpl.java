package kg.bakai.stopfactor.service.impl;

import kg.bakai.stopfactor.validator.StopFactorDefinitionValidator;
import kg.bakai.common.enums.ProductSegment;
import kg.bakai.common.exception.BusinessValidationException;
import kg.bakai.common.exception.NotFoundException;
import kg.bakai.stopfactor.util.NameTemplateRenderer;
import kg.bakai.stopfactor.util.Sentences;
import kg.bakai.stopfactor.service.StopFactorConsoleService;
import kg.bakai.stopfactor.dto.ConditionDto;
import kg.bakai.stopfactor.dto.RuleDto;
import kg.bakai.stopfactor.dto.StopFactorRequest;
import kg.bakai.stopfactor.dto.StopFactorResponse;
import kg.bakai.stopfactor.entity.StopFactor;
import kg.bakai.stopfactor.entity.StopFactorCondition;
import kg.bakai.stopfactor.entity.StopFactorMetric;
import kg.bakai.stopfactor.entity.StopFactorRule;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.repository.StopFactorMetricRepository;
import kg.bakai.stopfactor.repository.StopFactorRepository;
import kg.bakai.stopfactor.repository.projection.ProductProjection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StopFactorConsoleServiceImpl implements StopFactorConsoleService {

    private static final String CREATED_REASON = "Создание стоп-фактора";
    private static final String REASON_REQUIRED = "Укажите причину изменения: она сохраняется в истории версий.";

    private final StopFactorRepository stopFactorRepository;
    private final StopFactorMetricRepository metricRepository;
    private final StopFactorDefinitionValidator validator;

    @Override
    @Transactional(readOnly = true)
    public List<StopFactorResponse> getAll(Boolean active, Long metricId, Long productId, ProductSegment segment) {
        // a product that does not exist gets no stop factors with a segment or a product limit
        ProductSegment productSegment = productId == null ? null : stopFactorRepository.findProductsByIds(List.of(productId)).stream()
                .findFirst().map(ProductProjection::getSegment).orElse(null);
        List<StopFactor> stopFactors = stopFactorRepository.findAllCurrentWithMetric().stream()
                .filter(sf -> active == null || sf.isActive() == active)
                .filter(sf -> metricId == null || sf.getMetric().getId().equals(metricId))
                .filter(sf -> productId == null || sf.appliesTo(productId, productSegment))
                .filter(sf -> segment == null || sf.getSegment() == null || sf.getSegment() == segment)
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
            throw notFound(factorId);
        }
        return toResponses(versions);
    }

    @Override
    @Transactional
    public StopFactorResponse create(StopFactorRequest request, String user) {
        StopFactorMetric metric = metricRepository.findById(request.metricId())
                .orElseThrow(() -> new BusinessValidationException("Метрика с id " + request.metricId() + " не найдена."));

        List<String> errors = new ArrayList<>();
        if (!metric.isActive()) {
            errors.add("Метрика «" + metric.getTitle() + "» отключена. Включите её на странице метрик или выберите другую.");
        }
        Definition definition = validateDefinition(metric, request, errors);
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }

        StopFactor stopFactor = new StopFactor();
        stopFactor.setMetric(metric);
        addRules(stopFactor, definition.rules(), Map.of());
        String requestedName = blankToNull(request.name());
        stopFactor.setName(requestedName != null ? requestedName : defaultName(stopFactor));
        stopFactor.setDescription(blankToNull(request.description()));
        stopFactor.setActive(request.active() == null || request.active());
        stopFactor.setOrderNumber(request.orderNumber() != null
                ? request.orderNumber()
                : stopFactorRepository.findMaxCurrentOrderNumber() + 1);
        stopFactor.setProductIds(new HashSet<>(definition.productIds()));
        stopFactor.setSegment(request.segment());
        stopFactor.setValidFrom(LocalDateTime.now());
        stopFactor.setChangeReason(request.changeReason() != null && !request.changeReason().isBlank()
                ? request.changeReason().trim()
                : CREATED_REASON);
        stopFactor.setCreatedBy(user);

        StopFactor saved = stopFactorRepository.save(stopFactor);
        log.info("Стоп-фактор {} '{}' ({} правил) создан пользователем {}", saved.getFactorId(), saved.getName(),
                saved.getRules().size(), user);
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
            errors.add("Метрику стоп-фактора изменить нельзя. Чтобы проверять другую метрику, создайте новый стоп-фактор.");
        }
        if (request.changeReason() == null || request.changeReason().isBlank()) {
            errors.add(REASON_REQUIRED);
        }
        if (active && !current.isActive() && !metric.isActive()) {
            errors.add(metricOff(metric));
        }
        Definition definition = validateDefinition(metric, request, errors);
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }
        List<RuleDefinition> rules = definition.rules();
        Set<Long> productIds = definition.productIds();
        boolean rulesChanged = !ruleKeys(rules).equals(ruleKeysOf(current.getRules())) || renamesRules(rules, current.getRules());
        String requestedName = blankToNull(request.name());
        String description = blankToNull(request.description());

        boolean unchanged = !rulesChanged
                && (requestedName == null || requestedName.equals(current.getName()))
                && Objects.equals(description, current.getDescription())
                && orderNumber == current.getOrderNumber()
                && active == current.isActive()
                && productIds.equals(current.getProductIds())
                && request.segment() == current.getSegment();
        if (unchanged) {
            log.info("Стоп-фактор {}: изменений от {} нет, новая версия не создана", factorId, user);
            return get(factorId);
        }

        StopFactor next = closeAndCreateNextVersion(current, request.changeReason(), user);
        if (rulesChanged) {
            Map<String, String> currentNames = current.getRules().stream()
                    .collect(Collectors.toMap(rule -> paramsKey(rule.getParams()), StopFactorRule::getName, (a, b) -> a));
            next.removeRules();
            addRules(next, rules, currentNames);
        }
        next.setName(requestedName != null ? requestedName : rulesChanged ? defaultName(next) : current.getName());
        next.setDescription(description);
        next.setOrderNumber(orderNumber);
        next.setActive(active);
        next.setProductIds(new HashSet<>(productIds));
        next.setSegment(request.segment());
        StopFactor saved = stopFactorRepository.save(next);
        log.info("Стоп-фактор {} обновлен до версии {} пользователем {}: {}", factorId, saved.getVersionNumber(), user, request.changeReason());
        return get(factorId);
    }

    @Override
    @Transactional
    public StopFactorResponse changeStatus(long factorId, boolean active, String changeReason, String user) {
        if (changeReason == null || changeReason.isBlank()) {
            throw new BusinessValidationException(REASON_REQUIRED);
        }
        StopFactor current = lockAndFindCurrent(factorId);
        if (current.isActive() == active) {
            return get(factorId);
        }
        if (active && !current.getMetric().isActive()) {
            throw new BusinessValidationException(metricOff(current.getMetric()));
        }

        StopFactor next = closeAndCreateNextVersion(current, changeReason, user);
        next.setActive(active);
        StopFactor saved = stopFactorRepository.save(next);
        log.info("Стоп-фактор {} {} пользователем {} (версия {}): {}", factorId, active ? "активирован" : "деактивирован",
                user, saved.getVersionNumber(), changeReason);
        return get(factorId);
    }

    @Override
    @Transactional
    // The order does not change any decision, so it is updated in place without a new version
    public List<StopFactorResponse> reorder(List<Long> factorIds) {
        List<StopFactor> currents = stopFactorRepository.findAllCurrentWithMetric();
        Map<Long, StopFactor> byFactorId = currents.stream()
                .collect(Collectors.toMap(StopFactor::getFactorId, sf -> sf));

        List<String> errors = new ArrayList<>();
        Set<Long> requested = new LinkedHashSet<>();
        for (Long factorId : factorIds) {
            if (factorId == null) {
                errors.add("Список стоп-факторов содержит пустое значение.");
            } else if (!requested.add(factorId)) {
                errors.add("Стоп-фактор №" + factorId + " указан в списке несколько раз.");
            } else if (!byFactorId.containsKey(factorId)) {
                errors.add("Стоп-фактор №" + factorId + " не найден.");
            }
        }
        byFactorId.keySet().stream()
                .filter(id -> !requested.contains(id))
                .sorted()
                .forEach(id -> errors.add("В списке нет стоп-фактора №" + id + ": порядок задаётся сразу для всех стоп-факторов."));
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

    @Override
    @Transactional
    public void delete(long factorId, String changeReason, String user) {
        if (changeReason == null || changeReason.isBlank()) {
            throw new BusinessValidationException("Укажите причину удаления: она сохраняется в истории версий.");
        }
        StopFactor current = lockAndFindCurrent(factorId);
        StopFactor last = closeAndCreateNextVersion(current, changeReason, user);
        last.setActive(false);
        last.setValidTo(last.getValidFrom());
        StopFactor saved = stopFactorRepository.save(last);
        log.info("Стоп-фактор {} удален пользователем {} (версия {}): {}", factorId, user, saved.getVersionNumber(), changeReason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleDto> getRules(long factorId) {
        return get(factorId).rules();
    }

    @Override
    @Transactional(readOnly = true)
    public RuleDto getRule(long factorId, long ruleId) {
        List<RuleDto> rules = getRules(factorId);
        return rules.get(indexOfRule(factorId, rules, ruleId));
    }

    @Override
    @Transactional
    public StopFactorResponse addRule(long factorId, RuleDto rule, String changeReason, String user) {
        return changeRules(factorId, rules -> {
            rules.add(rule);
            return rules;
        }, changeReason, user);
    }

    @Override
    @Transactional
    public StopFactorResponse updateRule(long factorId, long ruleId, RuleDto rule, String changeReason, String user) {
        return changeRules(factorId, rules -> {
            rules.set(indexOfRule(factorId, rules, ruleId), rule);
            return rules;
        }, changeReason, user);
    }

    @Override
    @Transactional
    public StopFactorResponse deleteRule(long factorId, long ruleId, String changeReason, String user) {
        return changeRules(factorId, rules -> {
            int index = indexOfRule(factorId, rules, ruleId);
            if (rules.size() == 1) {
                throw new BusinessValidationException("Нельзя удалить единственное правило стоп-фактора №" + factorId
                        + ". Чтобы прекратить проверку, отключите или удалите стоп-фактор.");
            }
            rules.remove(index);
            return rules;
        }, changeReason, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConditionDto> getConditions(long factorId, long ruleId) {
        return getRule(factorId, ruleId).conditions();
    }

    @Override
    @Transactional
    public StopFactorResponse saveCondition(long factorId, long ruleId, ConditionDto condition, String changeReason, String user) {
        return changeRule(factorId, ruleId, conditions -> {
            conditions.removeIf(c -> c.result() == condition.result());
            conditions.add(condition);
            return conditions;
        }, changeReason, user);
    }

    @Override
    @Transactional
    public StopFactorResponse deleteCondition(long factorId, long ruleId, StopFactorStatus result, String changeReason, String user) {
        return changeRule(factorId, ruleId, conditions -> {
            if (!conditions.removeIf(c -> c.result() == result)) {
                throw new NotFoundException("У правила нет условия «" + result.getLabel() + "».");
            }
            return conditions;
        }, changeReason, user);
    }

    /** Changes the conditions of one rule; the rule keeps its name, params and status. */
    private StopFactorResponse changeRule(long factorId, long ruleId, UnaryOperator<List<ConditionDto>> change,
                                          String changeReason, String user) {
        return changeRules(factorId, rules -> {
            int index = indexOfRule(factorId, rules, ruleId);
            RuleDto rule = rules.get(index);
            rules.set(index, new RuleDto(rule.id(), rule.name(), rule.params(),
                    change.apply(new ArrayList<>(rule.conditions())), rule.active()));
            return rules;
        }, changeReason, user);
    }

    /**
     * Changes the rules of the current version through {@link #update}, so they are validated and versioned the same
     * way. The stop factor and the other rules keep their names.
     */
    private StopFactorResponse changeRules(long factorId, UnaryOperator<List<RuleDto>> change, String changeReason, String user) {
        lockAndFindCurrent(factorId);
        StopFactorResponse current = get(factorId);
        List<RuleDto> rules = change.apply(new ArrayList<>(current.rules()));
        return update(factorId, new StopFactorRequest(current.metricId(), current.name(), current.description(),
                current.orderNumber(), current.active(), current.productIds(), current.segment(), rules, changeReason), user);
    }

    private static int indexOfRule(long factorId, List<RuleDto> rules, long ruleId) {
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).id() != null && rules.get(i).id() == ruleId) {
                return i;
            }
        }
        throw new NotFoundException("Правило не найдено в текущей версии стоп-фактора №" + factorId
                + ". Возможно, стоп-фактор уже изменён: обновите страницу и повторите действие.");
    }

    /** A rule name given in the request that differs from the current one; a name left out keeps the current one. */
    private static boolean renamesRules(List<RuleDefinition> rules, List<StopFactorRule> current) {
        if (rules.size() != current.size()) {
            return false;
        }
        for (int i = 0; i < rules.size(); i++) {
            String name = rules.get(i).name();
            if (name != null && !name.equals(current.get(i).getName())) {
                return true;
            }
        }
        return false;
    }

    /** Rules and products of a request, normalized; problems are added to errors. */
    private record Definition(List<RuleDefinition> rules, Set<Long> productIds) {
    }

    /** A rule of a request, normalized; name is null when the request left it out. */
    private record RuleDefinition(String name, Map<String, Object> params, List<ConditionDto> conditions, boolean active) {
    }

    private Definition validateDefinition(StopFactorMetric metric, StopFactorRequest request, List<String> errors) {
        List<RuleDto> requestRules = request.rules() != null ? request.rules() : List.of();
        if (requestRules.isEmpty()) {
            errors.add("Добавьте хотя бы одно правило.");
        }
        List<RuleDefinition> rules = new ArrayList<>();
        Map<Map<String, Object>, Integer> ruleByParams = new HashMap<>();
        for (int i = 0; i < requestRules.size(); i++) {
            RuleDto rule = requestRules.get(i);
            String prefix = "Правило " + (i + 1) + ": ";
            List<String> ruleErrors = new ArrayList<>();
            Map<String, Object> params = validator.validateParams(metric, rule.params(), ruleErrors);
            List<ConditionDto> conditions = validator.validateConditions(metric,
                    rule.conditions() != null ? rule.conditions() : List.of(), ruleErrors);
            ruleErrors.forEach(error -> errors.add(prefix + Sentences.lowerFirst(error)));
            if (ruleErrors.isEmpty()) {
                Integer same = ruleByParams.putIfAbsent(params, i);
                if (same != null) {
                    errors.add("Правило " + (i + 1) + " повторяет правило " + (same + 1) + ": у них одинаковые параметры."
                            + " Измените параметры одного из правил или удалите лишнее.");
                }
            }
            rules.add(new RuleDefinition(blankToNull(rule.name()), params, conditions, rule.active() == null || rule.active()));
        }
        Set<Long> productIds = request.productIds() != null ? request.productIds() : Set.of();
        validator.validateProducts(productIds, errors);
        return new Definition(rules, productIds);
    }

    /**
     * A rule name left out is the name of the current rule with the same params, so a change of one rule does not
     * rename the others, or the metric's name template rendered with the params.
     */
    private static void addRules(StopFactor stopFactor, List<RuleDefinition> rules, Map<String, String> currentNames) {
        for (RuleDefinition definition : rules) {
            StopFactorRule rule = stopFactor.addRule();
            rule.setParams(definition.params());
            rule.setActive(definition.active());
            String name = definition.name() != null ? definition.name() : currentNames.get(paramsKey(definition.params()));
            rule.setName(name != null
                    ? name
                    : NameTemplateRenderer.render(stopFactor.getMetric().getNameTemplate(), definition.params()));
            definition.conditions().forEach(c ->
                    rule.addCondition(c.result(), c.valueType(), c.value(), c.operator(), c.valueTo()));
        }
    }

    /** A stop factor named in no request: like its only rule, or like its metric when it has several rules. */
    private static String defaultName(StopFactor stopFactor) {
        return stopFactor.getRules().size() == 1
                ? stopFactor.getRules().getFirst().getName()
                : stopFactor.getMetric().getTitle();
    }

    // The current version is closed and flushed before the next one is inserted: a stop factor has only one
    // current version (ux_stop_factor_current_version), and Hibernate would run the INSERT before the UPDATE
    private StopFactor closeAndCreateNextVersion(StopFactor current, String changeReason, String user) {
        LocalDateTime now = LocalDateTime.now();
        current.setValidTo(now);
        stopFactorRepository.saveAndFlush(current);

        StopFactor next = current.newVersion(now, changeReason.trim());
        next.setCreatedBy(user);
        return next;
    }

    private StopFactor lockAndFindCurrent(long factorId) {
        stopFactorRepository.lockFactor(factorId)
                .orElseThrow(() -> notFound(factorId));
        return findCurrent(factorId);
    }

    private StopFactor findCurrent(long factorId) {
        return stopFactorRepository.findCurrent(factorId)
                .orElseThrow(() -> notFound(factorId));
    }

    private static NotFoundException notFound(long factorId) {
        return new NotFoundException("Стоп-фактор №" + factorId + " не найден.");
    }

    private static String metricOff(StopFactorMetric metric) {
        return "Стоп-фактор нельзя включить: его метрика «" + metric.getTitle() + "» отключена. Сначала включите метрику.";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** What makes two rule lists the same: order, params and conditions (names follow from the params). */
    private static List<String> ruleKeys(List<RuleDefinition> rules) {
        return rules.stream()
                .map(rule -> rule.active() + "|" + new TreeMap<>(rule.params()) + "|" + conditionKeys(rule.conditions().stream()
                        .map(c -> c.result() + "|" + c.valueType() + "|" + c.value() + "|" + c.operator() + "|" + c.valueTo())
                        .toList()))
                .toList();
    }

    private static String paramsKey(Map<String, Object> params) {
        return new TreeMap<>(params).toString();
    }

    private static List<String> ruleKeysOf(List<StopFactorRule> rules) {
        return rules.stream()
                .map(rule -> rule.isActive() + "|" + new TreeMap<>(rule.getParams()) + "|" + conditionKeys(rule.getConditions().stream()
                        .map(c -> c.getResult() + "|" + c.getValueType() + "|" + c.getValue() + "|" + c.getOperator() + "|" + c.getValueTo())
                        .toList()))
                .toList();
    }

    private static Set<String> conditionKeys(List<String> keys) {
        return new TreeSet<>(keys);
    }

    private List<StopFactorResponse> toResponses(List<StopFactor> stopFactors) {
        return stopFactors.stream().map(StopFactorConsoleServiceImpl::toResponse).toList();
    }

    private static StopFactorResponse toResponse(StopFactor sf) {
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
                new TreeSet<>(sf.getProductIds()),
                sf.getSegment(),
                sf.getRules().stream().map(StopFactorConsoleServiceImpl::toDto).toList(),
                sf.getValidFrom(),
                sf.getValidTo(),
                sf.getChangeReason(),
                sf.getCreatedBy(),
                sf.getCreatedDate()
        );
    }

    private static RuleDto toDto(StopFactorRule rule) {
        return new RuleDto(rule.getId(), rule.getName(), new TreeMap<>(rule.getParams()),
                rule.getConditions().stream()
                        .sorted(Comparator.comparing(StopFactorCondition::getResult))
                        .map(c -> new ConditionDto(c.getId(), c.getResult(), c.getValueType(), c.getValue(),
                                c.getOperator(), c.getValueTo()))
                        .toList(),
                rule.isActive());
    }
}
