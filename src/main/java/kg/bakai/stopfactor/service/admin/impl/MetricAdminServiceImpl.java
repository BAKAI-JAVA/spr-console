package kg.bakai.stopfactor.service.admin.impl;

import kg.bakai.stopfactor.calculator.CalculatorRegistry;
import kg.bakai.stopfactor.calculator.ParamSpec;
import kg.bakai.stopfactor.calculator.StopFactorCalculator;
import kg.bakai.stopfactor.dto.admin.CalculatorDto;
import kg.bakai.stopfactor.dto.admin.MetricDto;
import kg.bakai.stopfactor.dto.admin.MetricParamSchema;
import kg.bakai.stopfactor.entity.StopFactor;
import kg.bakai.stopfactor.entity.StopFactorMetric;
import kg.bakai.stopfactor.enums.CalculatorType;
import kg.bakai.stopfactor.evaluation.NameTemplateRenderer;
import kg.bakai.stopfactor.exception.BusinessValidationException;
import kg.bakai.stopfactor.exception.ConflictException;
import kg.bakai.stopfactor.exception.NotFoundException;
import kg.bakai.stopfactor.repository.StopFactorMetricRepository;
import kg.bakai.stopfactor.repository.StopFactorRepository;
import kg.bakai.stopfactor.service.admin.MetricAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricAdminServiceImpl implements MetricAdminService {

    private final StopFactorMetricRepository metricRepository;
    private final StopFactorRepository stopFactorRepository;
    private final CalculatorRegistry calculatorRegistry;
    private final StopFactorDefinitionValidator validator;

    @Override
    public List<CalculatorDto> getCalculators() {
        return Arrays.stream(CalculatorType.values())
                .map(calculatorRegistry::get)
                .map(calculator -> new CalculatorDto(
                        calculator.type(),
                        calculator.type().getName(),
                        calculator.valueKind(),
                        new TreeSet<>(calculator.valueKind().conditionTypes()),
                        calculator.paramSpecs().stream()
                                .map(spec -> new CalculatorDto.CalculatorParam(
                                        spec.key(),
                                        spec.label(),
                                        spec.required(),
                                        spec.isInteger() ? "INTEGER" : "ENUM",
                                        spec.isInteger() ? spec.min() : null,
                                        spec.isInteger() ? null : spec.allowedValues()))
                                .toList()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MetricDto> getAll() {
        return metricRepository.findAll(Sort.by("id")).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MetricDto get(long id) {
        return toDto(find(id));
    }

    @Override
    @Transactional
    public MetricDto create(MetricDto request) {
        String title = request.title().trim();
        requireUniqueTitle(title, null);
        StopFactorCalculator calculator = calculatorRegistry.get(request.calculator());
        List<MetricParamSchema> params = request.params() != null
                ? normalizeSchema(request.params())
                : calculator.paramSpecs().stream().map(ParamSpec::toSchema).toList();
        validateDefinition(calculator, request.nameTemplate(), params);

        StopFactorMetric metric = new StopFactorMetric();
        metric.setTitle(title);
        metric.setCalculator(request.calculator());
        metric.setNameTemplate(request.nameTemplate().trim());
        metric.setParams(new ArrayList<>(params));
        metric.setActive(request.active() == null || request.active());
        StopFactorMetric saved = metricRepository.save(metric);
        log.info("Метрика {} '{}' ({}) создана", saved.getId(), saved.getTitle(), saved.getCalculator());
        return toDto(saved);
    }

    @Override
    @Transactional
    public MetricDto update(long id, MetricDto request) {
        StopFactorMetric metric = find(id);
        if (request.calculator() != metric.getCalculator()) {
            throw new BusinessValidationException("calculator: калькулятор нельзя изменить, создайте новую метрику");
        }
        String title = request.title().trim();
        requireUniqueTitle(title, id);
        StopFactorCalculator calculator = calculatorRegistry.get(metric.getCalculator());
        List<MetricParamSchema> params = request.params() != null ? normalizeSchema(request.params()) : metric.getParams();
        validateDefinition(calculator, request.nameTemplate(), params);

        StopFactorMetric candidate = new StopFactorMetric();
        candidate.setTitle(title);
        candidate.setCalculator(metric.getCalculator());
        candidate.setParams(params);
        List<String> errors = new ArrayList<>();
        for (StopFactor stopFactor : stopFactorRepository.findCurrentByMetricId(id)) {
            List<String> factorErrors = new ArrayList<>();
            validator.validateParams(candidate, stopFactor.getParams(), factorErrors);
            factorErrors.forEach(e -> errors.add("Стоп-фактор " + stopFactor.getFactorId() + ": " + e));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }

        metric.setTitle(title);
        metric.setNameTemplate(request.nameTemplate().trim());
        metric.setParams(new ArrayList<>(params));
        if (request.active() != null) {
            metric.setActive(request.active());
        }
        log.info("Метрика {} обновлена (active={})", id, metric.isActive());
        return toDto(metricRepository.saveAndFlush(metric));
    }

    @Override
    @Transactional
    public void delete(long id) {
        StopFactorMetric metric = find(id);
        if (stopFactorRepository.existsByMetric_Id(id)) {
            throw new ConflictException("Метрика " + id + " используется стоп-факторами и не может быть удалена, деактивируйте ее");
        }
        metricRepository.delete(metric);
        log.info("Метрика {} '{}' удалена", id, metric.getTitle());
    }

    private void validateDefinition(StopFactorCalculator calculator, String nameTemplate, List<MetricParamSchema> params) {
        List<String> errors = new ArrayList<>();
        Map<String, ParamSpec> specs = calculator.paramSpecs().stream()
                .collect(Collectors.toMap(ParamSpec::key, s -> s));

        Set<String> keys = new HashSet<>();
        for (MetricParamSchema param : params) {
            if (!keys.add(param.key())) {
                errors.add("params: ключ '" + param.key() + "' повторяется");
            } else if (!specs.containsKey(param.key())) {
                errors.add("params: '" + param.key() + "' не поддерживается калькулятором " + calculator.type()
                        + ", поддерживаются: " + new TreeSet<>(specs.keySet()));
            }
        }
        specs.values().stream()
                .filter(ParamSpec::required)
                .forEach(spec -> params.stream()
                        .filter(p -> p.key().equals(spec.key()))
                        .findFirst()
                        .ifPresentOrElse(
                                p -> {
                                    if (!p.required()) {
                                        errors.add("params: '" + spec.key() + "' должен быть обязательным для " + calculator.type());
                                    }
                                },
                                () -> errors.add("params: '" + spec.key() + "' обязателен для " + calculator.type())));

        NameTemplateRenderer.placeholders(nameTemplate).stream()
                .filter(key -> !keys.contains(key))
                .forEach(key -> errors.add("nameTemplate: {" + key + "} не является параметром этой метрики"));

        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }
    }

    private List<MetricParamSchema> normalizeSchema(List<MetricParamSchema> params) {
        return params.stream()
                .map(p -> new MetricParamSchema(p.key().trim(), p.label().trim(), p.required()))
                .toList();
    }

    // Сравнение в Java: при локали C базы данных lower() в PostgreSQL не меняет кириллицу
    private void requireUniqueTitle(String title, Long exceptId) {
        boolean taken = metricRepository.findAll().stream()
                .anyMatch(m -> !m.getId().equals(exceptId) && m.getTitle().equalsIgnoreCase(title));
        if (taken) {
            throw new ConflictException("Метрика '" + title + "' уже существует");
        }
    }

    private StopFactorMetric find(long id) {
        return metricRepository.findById(id).orElseThrow(() -> new NotFoundException("Метрика " + id + " не найдена"));
    }

    private MetricDto toDto(StopFactorMetric metric) {
        return new MetricDto(metric.getId(), metric.getTitle(), metric.getCalculator(),
                metric.getCalculator().getName(), metric.getNameTemplate(), List.copyOf(metric.getParams()),
                metric.isActive(), metric.getCreatedDate(), metric.getUpdatedDate());
    }
}
