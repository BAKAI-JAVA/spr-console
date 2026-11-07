package kg.bakai.stopfactor;

import jakarta.persistence.EntityManager;
import kg.bakai.common.exception.BusinessValidationException;
import kg.bakai.stopfactor.dto.StopFactorConditionDto;
import kg.bakai.stopfactor.service.StopFactorService;
import kg.bakai.stopfactor.service.MetricConsoleService;
import kg.bakai.stopfactor.service.StopFactorConsoleService;
import kg.bakai.stopfactor.dto.ConditionDto;
import kg.bakai.stopfactor.dto.RuleDto;
import kg.bakai.stopfactor.dto.MetricDto;
import kg.bakai.stopfactor.dto.StopFactorRequest;
import kg.bakai.stopfactor.dto.StopFactorResponse;
import kg.bakai.stopfactor.entity.MetricParamSchema;
import kg.bakai.stopfactor.enums.ConditionOperator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static kg.bakai.stopfactor.enums.CalculatorType.OVERDUE_BUCKET_COUNT;
import static kg.bakai.stopfactor.enums.StopFactorStatus.REJECTED;
import static kg.bakai.stopfactor.enums.StopFactorStatus.UNDERWRITER;
import static kg.bakai.stopfactor.enums.StopFactorValueTypes.INTEGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Stop factors made of rules (changelog 1.7): the params JSON of a metric holds what it is set up with,
 * stop_factor_rule the rules of a stop factor version with their param values (params JSON). Every test is rolled back.
 */
@SpringBootTest
@Transactional
class StopFactorRulesTest {

    private static final long OVERDUE_METRIC = 10;
    private static final long RESTRUCTURING_METRIC = 4;

    @Autowired
    private MetricConsoleService metricConsoleService;
    @Autowired
    private StopFactorConsoleService stopFactorConsoleService;
    @Autowired
    private StopFactorService stopFactorService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    // ------------------------------------------------------------------------------------------------
    // Changelog 1.7
    // ------------------------------------------------------------------------------------------------

    @Test
    void metricParamsAreJsonInTheirOrder() {
        assertThat(metricConsoleService.get(OVERDUE_METRIC).params())
                .extracting(p -> p.key() + " " + p.required())
                .containsExactly("periodFromMonth true", "periodToMonth true", "minOverdueDays true", "maxOverdueDays false");
        assertThat(metricConsoleService.get(RESTRUCTURING_METRIC).params()).extracting(MetricParamSchema::key)
                .containsExactly("source", "periodFromMonth", "periodToMonth");
    }

    @Test
    void everyLegacyStopFactorIsARuleWithItsValues() {
        // a rule of the legacy stop factors has the id of its legacy stop factor:
        // #4 KIB restructurings of months 1..12, #29 months 1..3 with 31+ days, #2 no params
        assertThat(values(4)).isEqualTo(Map.of("source", "KIB", "periodFromMonth", "1", "periodToMonth", "12"));
        assertThat(values(29)).isEqualTo(Map.of("periodFromMonth", "1", "periodToMonth", "3", "minOverdueDays", "31"));
        assertThat(values(2)).isEmpty();
        // the legacy stop factors that kept their own metric have one rule named like them
        for (long factorId : List.of(1L, 2L, 3L, 8L, 9L, 10L, 11L, 12L)) {
            StopFactorResponse legacy = stopFactorConsoleService.get(factorId);
            assertThat(legacy.rules()).as("stop factor " + factorId).singleElement()
                    .satisfies(rule -> assertThat(rule.name()).isEqualTo(legacy.name()));
        }
    }

    @Test
    void legacyStopFactorsOfOneMetricAreOneStopFactorWithRules() {
        StopFactorResponse restructurings = stopFactorConsoleService.get(4);
        assertThat(restructurings.name()).isEqualTo("Количество реструктуризаций");
        assertThat(restructurings.rules()).extracting(rule -> rule.params().get("source") + " " + rule.params().get("periodFromMonth"))
                .containsExactly("KIB 1", "OB 1", "KIB 13", "OB 13");

        // the legacy overdue history: version 1 with the legacy rules, version 2 inactive since the 2026 rules
        List<StopFactorResponse> overdue = stopFactorConsoleService.getVersions(13);
        assertThat(overdue).extracting(StopFactorResponse::versionNumber).containsExactly(2, 1);
        assertThat(overdue).allSatisfy(version -> {
            assertThat(version.name()).isEqualTo("Количество непрерывной просроченной задолженности");
            assertThat(version.rules()).hasSize(22);
        });
        assertThat(overdue.getFirst().active()).isFalse();
        assertThat(overdue.get(1).rules()).filteredOn(RuleDto::active).hasSize(17);
        assertThat(overdue.get(1).rules().getFirst().name()).endsWith("с 1 го по 3 месяц, где максимальная просрочка >= 1 и <= 15");

        // there are no separate stop factors #5..#7, #14..#34 any more
        assertThat(count("""
                SELECT count(*) FROM stop_factors.stop_factor
                WHERE COALESCE(root_id, id) BETWEEN 5 AND 7 OR COALESCE(root_id, id) BETWEEN 14 AND 34""")).isZero();
    }

    @Test
    void anInactiveRuleIsKeptButNotChecked() {
        StopFactorResponse created = stopFactorConsoleService.create(new StopFactorRequest(OVERDUE_METRIC, null, null, null, true,
                Set.of(), null, List.of(
                rule(Map.of("periodFromMonth", 1, "periodToMonth", 3, "minOverdueDays", 30)),
                new RuleDto(null, null, Map.of("periodFromMonth", 4, "periodToMonth", 6, "minOverdueDays", 30),
                        List.of(new ConditionDto(null, REJECTED, INTEGER, "0")), false)), null), "test");

        assertThat(created.rules()).extracting(RuleDto::active).containsExactly(true, false);
    }

    @Test
    void the2026OverdueHistoryIsOneStopFactorPerSegment() {
        StopFactorResponse retail = stopFactorConsoleService.get(overdueStopFactor("RETAIL"));

        assertThat(retail.name()).isEqualTo("Непрерывная просроченная задолженность");
        assertThat(retail.rules()).hasSize(14);
        // rule 7: months 7..12, 6..10 days - "до 5 - Ок, 6-8 - жёлтая зона, 9+ - отказ"
        RuleDto rule = retail.rules().get(6);
        assertThat(rule.params()).isEqualTo(Map.of("periodFromMonth", 7, "periodToMonth", 12, "minOverdueDays", 6, "maxOverdueDays", 10));
        assertThat(rule.name()).endsWith("с 7 по 12 месяц, где максимальная просрочка >= 6 и <= 10");
        assertThat(rule.conditions()).containsExactlyInAnyOrder(
                new ConditionDto(rule.conditions().get(0).id(), REJECTED, INTEGER, "8", ConditionOperator.GT, null),
                new ConditionDto(rule.conditions().get(1).id(), UNDERWRITER, INTEGER, "5", ConditionOperator.RANGE, "8"));
        assertThat(stopFactorConsoleService.get(overdueStopFactor("MSB")).rules()).hasSize(14);
    }

    @Test
    void everyValueIsAParamOfTheMetricOfItsStopFactor() {
        // every key of the params of a rule is a param of its metric
        assertThat(count("""
                SELECT count(*) FROM stop_factors.stop_factor_rule r
                JOIN stop_factors.stop_factor sf ON sf.id = r.stop_factor_id
                CROSS JOIN LATERAL jsonb_object_keys(r.params) AS k(key)
                JOIN stop_factors.stop_factor_metric m ON m.id = sf.metric_id
                WHERE NOT EXISTS (SELECT 1 FROM jsonb_array_elements(m.params) AS d(definition)
                                  WHERE d.definition ->> 'key' = k.key)""")).isZero();
        // every required param of the metric has a value in every rule
        assertThat(count("""
                SELECT count(*) FROM stop_factors.stop_factor_rule r
                JOIN stop_factors.stop_factor sf ON sf.id = r.stop_factor_id
                JOIN stop_factors.stop_factor_metric m ON m.id = sf.metric_id
                CROSS JOIN LATERAL jsonb_array_elements(m.params) AS d(definition)
                WHERE (d.definition ->> 'required')::boolean AND NOT jsonb_exists(r.params, d.definition ->> 'key')""")).isZero();
        // param definitions on the metric, values on the rules
        assertThat(jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.columns
                WHERE table_schema = 'stop_factors' AND column_name = 'params'""", String.class))
                .containsExactlyInAnyOrder("stop_factor_metric", "stop_factor_rule");
    }

    // ------------------------------------------------------------------------------------------------
    // Console
    // ------------------------------------------------------------------------------------------------

    @Test
    void aStopFactorIsSavedWithItsParamRows() {
        StopFactorResponse created = createOverdueStopFactor(Map.of("periodFromMonth", 1, "periodToMonth", 6, "minOverdueDays", 30));

        assertThat(values(created.rules().getFirst().id())).isEqualTo(Map.of("periodFromMonth", "1", "periodToMonth", "6", "minOverdueDays", "30"));
        assertThat(created.name()).isEqualTo(created.rules().getFirst().name()).isEqualTo(
                "Количество непрерывной просроченной задолженности по основному долгу и/или процентам с 1 по 6 месяц, "
                        + "где максимальная просрочка >= 30");
    }

    @Test
    void newParamsAreANewVersionAndTheOldVersionKeepsItsOwn() {
        StopFactorResponse created = createOverdueStopFactor(Map.of("periodFromMonth", 1, "periodToMonth", 6, "minOverdueDays", 30));

        StopFactorResponse updated = update(created, Map.of("periodFromMonth", 1, "periodToMonth", 6, "minOverdueDays", 30, "maxOverdueDays", 60));

        assertThat(updated.versionNumber()).isEqualTo(2);
        assertThat(values(updated.rules().getFirst().id())).containsEntry("maxOverdueDays", "60").hasSize(4);
        assertThat(values(created.rules().getFirst().id())).doesNotContainKey("maxOverdueDays").hasSize(3);
    }

    @Test
    void sameParamsCreateNoNewVersion() {
        StopFactorResponse created = createOverdueStopFactor(Map.of("periodFromMonth", 1, "periodToMonth", 6, "minOverdueDays", 30));

        StopFactorResponse updated = update(created, Map.of("periodFromMonth", "1", "periodToMonth", 6, "minOverdueDays", 30.0));

        assertThat(updated.versionNumber()).isEqualTo(1);
    }

    @Test
    void aMetricParamLabelChanges() {
        MetricDto metric = metricConsoleService.get(OVERDUE_METRIC);

        metricConsoleService.update(OVERDUE_METRIC, withParams(metric, metric.params().stream()
                .map(p -> p.key().equals("minOverdueDays") ? new MetricParamSchema(p.key(), "Просрочка от, дней", p.required()) : p)
                .toList()));
        entityManager.flush();

        assertThat(metricConsoleService.get(OVERDUE_METRIC).params())
                .contains(new MetricParamSchema("minOverdueDays", "Просрочка от, дней", true));
    }

    @Test
    void aMetricParamAnOldVersionUsesCannotBeRemoved() {
        MetricDto metric = metricConsoleService.create(new MetricDto(null, "Тест: просрочки", OVERDUE_BUCKET_COUNT, null,
                "Тест {periodFromMonth}-{periodToMonth}: {minOverdueDays}", null, true, null, null));
        StopFactorResponse created = stopFactorConsoleService.create(new StopFactorRequest(metric.id(), null, null, null, true,
                Set.of(), null, List.of(rule(Map.of("periodFromMonth", 1, "periodToMonth", 3, "minOverdueDays", 1, "maxOverdueDays", 10))),
                null), "test");
        update(created, Map.of("periodFromMonth", 1, "periodToMonth", 3, "minOverdueDays", 1));
        entityManager.flush();

        // the current version has no maxOverdueDays, the first one does
        List<MetricParamSchema> withoutMax = metric.params().stream().filter(p -> !p.key().equals("maxOverdueDays")).toList();
        assertThatThrownBy(() -> metricConsoleService.update(metric.id(), withParams(metric, withoutMax)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("params: [maxOverdueDays] заданы у стоп-факторов этой метрики");
    }

    @Test
    void anUnusedMetricParamIsRemoved() {
        MetricDto metric = metricConsoleService.create(new MetricDto(null, "Тест: просрочки без верхней границы", OVERDUE_BUCKET_COUNT,
                null, "Тест {periodFromMonth}-{periodToMonth}: {minOverdueDays}", null, true, null, null));
        List<MetricParamSchema> withoutMax = metric.params().stream().filter(p -> !p.key().equals("maxOverdueDays")).toList();

        MetricDto updated = metricConsoleService.update(metric.id(), withParams(metric, withoutMax));
        entityManager.flush();

        assertThat(updated.params()).extracting(MetricParamSchema::key)
                .containsExactly("periodFromMonth", "periodToMonth", "minOverdueDays");
        assertThat(count("SELECT jsonb_array_length(params) FROM stop_factors.stop_factor_metric WHERE id = " + metric.id())).isEqualTo(3);
    }

    // ------------------------------------------------------------------------------------------------
    // Several rules
    // ------------------------------------------------------------------------------------------------

    @Test
    void aStopFactorWithSeveralRules() {
        StopFactorResponse created = stopFactorConsoleService.create(new StopFactorRequest(OVERDUE_METRIC, null, "Тест", null, true,
                Set.of(), null, List.of(
                rule(Map.of("periodFromMonth", 1, "periodToMonth", 3, "minOverdueDays", 30)),
                new RuleDto(null, "Свое имя правила", Map.of("periodFromMonth", 4, "periodToMonth", 6, "minOverdueDays", 30),
                        List.of(new ConditionDto(null, UNDERWRITER, INTEGER, "0")))), null), "test");
        entityManager.flush();

        assertThat(created.name()).isEqualTo("Непрерывная просроченная задолженность");
        assertThat(created.rules()).extracting(RuleDto::name).containsExactly(
                "Количество непрерывной просроченной задолженности по основному долгу и/или процентам с 1 по 3 месяц, "
                        + "где максимальная просрочка >= 30",
                "Свое имя правила");
        assertThat(count("SELECT count(*) FROM stop_factors.stop_factor_rule WHERE stop_factor_id = " + created.versionId())).isEqualTo(2);
    }

    @Test
    void twoRulesWithTheSameParamsAreRefused() {
        Map<String, Object> params = Map.of("periodFromMonth", 1, "periodToMonth", 3, "minOverdueDays", 30);

        assertThatThrownBy(() -> stopFactorConsoleService.create(new StopFactorRequest(OVERDUE_METRIC, null, null, null, true,
                Set.of(), null, List.of(rule(params), rule(params)), null), "test"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("rules[1]: параметры совпадают с rules[0]");
        assertThatThrownBy(() -> stopFactorConsoleService.create(new StopFactorRequest(OVERDUE_METRIC, null, null, null, true,
                Set.of(), null, List.of(), null), "test"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("rules: нужно хотя бы одно правило");
    }

    @Test
    void changingOneRuleIsANewVersionWithAllRulesAndTheOldVersionKeepsItsOwn() {
        long factorId = overdueStopFactor("RETAIL");
        StopFactorResponse current = stopFactorConsoleService.get(factorId);
        List<RuleDto> rules = new java.util.ArrayList<>(current.rules());
        RuleDto first = rules.getFirst();
        rules.set(0, new RuleDto(null, null, first.params(), List.of(new ConditionDto(null, REJECTED, INTEGER, "6"))));

        StopFactorResponse updated = stopFactorConsoleService.update(factorId, new StopFactorRequest(current.metricId(), null,
                current.description(), current.orderNumber(), current.active(), current.productIds(), current.segment(),
                rules, "Новый порог правила 1"), "test");
        entityManager.flush();

        assertThat(updated.versionNumber()).isEqualTo(current.versionNumber() + 1);
        assertThat(updated.rules()).hasSize(14);
        assertThat(updated.rules().getFirst().conditions()).singleElement().extracting(ConditionDto::value).isEqualTo("6");
        assertThat(stopFactorConsoleService.getVersions(factorId).get(1).rules().getFirst().conditions())
                .extracting(ConditionDto::value).containsExactly("5");
        assertThat(updated.name()).isEqualTo(current.name());
    }

    @Test
    void legacySaveConditionNeedsAStopFactorWithOneRule() {
        assertThatThrownBy(() -> stopFactorService.saveCondition(
                new StopFactorConditionDto(overdueStopFactor("RETAIL"), "3", INTEGER, REJECTED)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("состоит из 14 правил");
    }

    // ------------------------------------------------------------------------------------------------

    private static RuleDto rule(Map<String, Object> params) {
        return new RuleDto(params, List.of(new ConditionDto(null, REJECTED, INTEGER, "0")));
    }

    private StopFactorResponse createOverdueStopFactor(Map<String, Object> params) {
        StopFactorResponse created = stopFactorConsoleService.create(new StopFactorRequest(OVERDUE_METRIC, null, null, null, true,
                Set.of(), null, List.of(rule(params)), null), "test");
        entityManager.flush();
        return created;
    }

    private StopFactorResponse update(StopFactorResponse current, Map<String, Object> params) {
        StopFactorResponse updated = stopFactorConsoleService.update(current.factorId(), new StopFactorRequest(current.metricId(),
                null, current.description(), current.orderNumber(), current.active(), current.productIds(),
                current.segment(), List.of(rule(params)), "Тест параметров"), "test");
        entityManager.flush();
        return updated;
    }

    /** The 2026 overdue history stop factor of a segment. */
    private long overdueStopFactor(String segment) {
        Long id = jdbcTemplate.queryForObject("""
                SELECT COALESCE(sf.root_id, sf.id) FROM stop_factors.stop_factor sf
                WHERE sf.valid_to IS NULL AND sf.segment = ? AND sf.change_reason = 'Правила по просрочкам 2026'""",
                Long.class, segment);
        return id != null ? id : 0;
    }

    private static MetricDto withParams(MetricDto metric, List<MetricParamSchema> params) {
        return new MetricDto(metric.id(), metric.title(), metric.calculator(), null, metric.nameTemplate(), params,
                metric.active(), null, null);
    }

    /** Values of a rule as stored. */
    private Map<String, String> values(long ruleId) {
        return jdbcTemplate.query("""
                        SELECT p.key, p.value FROM stop_factors.stop_factor_rule r
                        CROSS JOIN LATERAL jsonb_each_text(r.params) AS p(key, value)
                        WHERE r.id = ?""",
                rs -> {
                    Map<String, String> values = new java.util.HashMap<>();
                    while (rs.next()) {
                        values.put(rs.getString(1), rs.getString(2));
                    }
                    return values;
                }, ruleId);
    }

    private long count(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }
}
