package kg.bakai.stopfactor;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import kg.bakai.common.exception.BusinessValidationException;
import kg.bakai.stopfactor.dto.console.ConditionDto;
import kg.bakai.stopfactor.dto.console.StopFactorRequest;
import kg.bakai.stopfactor.dto.console.StopFactorResponse;
import kg.bakai.stopfactor.dto.scoring.StopFactorConditionDto;
import kg.bakai.stopfactor.enums.ConditionOperator;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.service.StopFactorService;
import kg.bakai.stopfactor.service.console.StopFactorConsoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static kg.bakai.stopfactor.enums.ConditionOperator.BETWEEN;
import static kg.bakai.stopfactor.enums.ConditionOperator.EQ;
import static kg.bakai.stopfactor.enums.ConditionOperator.GT;
import static kg.bakai.stopfactor.enums.ConditionOperator.GTE;
import static kg.bakai.stopfactor.enums.ConditionOperator.LT;
import static kg.bakai.stopfactor.enums.ConditionOperator.LTE;
import static kg.bakai.stopfactor.enums.ConditionOperator.IN;
import static kg.bakai.stopfactor.enums.ConditionOperator.RANGE;
import static kg.bakai.stopfactor.enums.StopFactorStatus.REJECTED;
import static kg.bakai.stopfactor.enums.StopFactorStatus.UNDERWRITER;
import static kg.bakai.stopfactor.enums.StopFactorValueTypes.ARRAY;
import static kg.bakai.stopfactor.enums.StopFactorValueTypes.INTEGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Condition operator and value_to (changelog 1.6): how the migration filled them, how the console and the legacy
 * saveCondition set them, and the database constraints. Every test is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConditionOperatorTest {

    private static final long KIB_REFUSAL_STATUSES_METRIC = 1;
    private static final long ACTIVE_DISPUTES_METRIC = 2;

    @Autowired
    private StopFactorConsoleService consoleService;
    @Autowired
    private StopFactorService stopFactorService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------------------------------------------
    // Changelog 1.6
    // ------------------------------------------------------------------------------------------------

    @Test
    void migrationFilledEveryCondition() {
        // conditions of the stop factors created by the changelogs; console edits may use any operator
        String migrated = """
                (SELECT c.* FROM stop_factors.stop_factor_condition c
                 JOIN stop_factors.stop_factor sf ON sf.id = c.stop_factor_id AND sf.created_by = 'migration')""";

        assertThat(count("SELECT count(*) FROM " + migrated + " c")).isPositive();
        assertThat(count("SELECT count(*) FROM stop_factors.stop_factor_condition WHERE operator IS NULL")).isZero();
        assertThat(count("SELECT count(*) FROM " + migrated + " c WHERE (value_type IN ('ARRAY', 'STRING')) <> (operator = 'IN')")).isZero();
        assertThat(count("SELECT count(*) FROM " + migrated + """
                 c WHERE result = 'REJECTED' AND operator <> 'GT' AND value_type IN ('INTEGER', 'DOUBLE')""")).isZero();

        // every UNDERWRITER number with a REJECTED number is RANGE up to exactly that REJECTED value
        assertThat(count("SELECT count(*) FROM " + migrated + " uw JOIN " + migrated + """
                 rej ON rej.stop_factor_id = uw.stop_factor_id AND rej.result = 'REJECTED'
                WHERE uw.result = 'UNDERWRITER' AND rej.value_type IN ('INTEGER', 'DOUBLE')
                  AND (uw.operator <> 'RANGE' OR uw.value_to <> rej.value)""")).isZero();
        // every other condition has no value_to
        assertThat(count("SELECT count(*) FROM " + migrated + " c WHERE (operator = 'RANGE') <> (value_to IS NOT NULL)")).isZero();
    }

    @Test
    void retailRuleSixToEightIsTheUnderwriterRange() {
        // changelog 1.4: months 7..12, overdue of 6..10 days - "до 5 - Ок, 6-8 - жёлтая зона, 9+ - отказ"
        List<String> conditions = jdbcTemplate.queryForList("""
                SELECT c.result || ' ' || c.operator || ' ' || c.value || ' ' || coalesce(c.value_to, '-')
                FROM stop_factors.stop_factor sf
                JOIN stop_factors.stop_factor_condition c ON c.stop_factor_id = sf.id
                WHERE sf.valid_to IS NULL AND sf.segment = 'RETAIL'
                  AND sf.params = '{"periodFromMonth": 7, "periodToMonth": 12, "minOverdueDays": 6, "maxOverdueDays": 10}'::jsonb
                ORDER BY c.result""", String.class);

        assertThat(conditions).containsExactly("REJECTED GT 8 -", "UNDERWRITER RANGE 5 8");
    }

    // ------------------------------------------------------------------------------------------------
    // Console
    // ------------------------------------------------------------------------------------------------

    @Test
    void consoleSetsTheOperatorsFromTheConditions() {
        StopFactorResponse created = create(new ConditionDto(null, UNDERWRITER, INTEGER, "5"), new ConditionDto(null, REJECTED, INTEGER, "8"));

        assertThat(withoutIds(created)).containsExactlyInAnyOrder(
                new ConditionDto(null, REJECTED, INTEGER, "8", GT, null),
                new ConditionDto(null, UNDERWRITER, INTEGER, "5", RANGE, "8"));
        assertThat(jdbcTemplate.queryForList(
                "SELECT result, operator, value, value_to FROM stop_factors.stop_factor_condition WHERE stop_factor_id = ? ORDER BY result",
                created.versionId()))
                .extracting(row -> row.get("result") + " " + row.get("operator") + " " + row.get("value") + " " + row.get("value_to"))
                .containsExactly("REJECTED GT 8 null", "UNDERWRITER RANGE 5 8");
    }

    @Test
    void underwriterAloneIsGreaterThan() {
        StopFactorResponse created = create(new ConditionDto(null, UNDERWRITER, INTEGER, "0"));

        assertThat(withoutIds(created)).containsExactly(new ConditionDto(null, UNDERWRITER, INTEGER, "0", GT, null));
    }

    @Test
    void loanStatusesAreIn() {
        StopFactorResponse created = consoleService.create(new StopFactorRequest(KIB_REFUSAL_STATUSES_METRIC,
                "Тест: статусы", null, null, true, Map.of(), Set.of(), null,
                List.of(new ConditionDto(null, REJECTED, ARRAY, "WrittenOff"),
                        new ConditionDto(null, UNDERWRITER, ARRAY, "TransferredToOtherReportingInst")),
                null), "test");

        assertThat(created.conditions()).extracting(ConditionDto::operator).containsOnly(IN);
        assertThat(created.conditions()).extracting(ConditionDto::valueTo).containsOnlyNulls();
    }

    @Test
    void responseSentBackUnchangedCreatesNoNewVersion() {
        StopFactorResponse created = create(new ConditionDto(null, UNDERWRITER, INTEGER, "5"), new ConditionDto(null, REJECTED, INTEGER, "8"));

        StopFactorResponse updated = update(created, created.conditions());

        assertThat(updated.versionNumber()).isEqualTo(created.versionNumber());
    }

    @Test
    void newRejectedValueMovesTheUnderwriterRange() {
        StopFactorResponse created = create(new ConditionDto(null, UNDERWRITER, INTEGER, "5"), new ConditionDto(null, REJECTED, INTEGER, "8"));

        StopFactorResponse updated = update(created, List.of(
                new ConditionDto(null, UNDERWRITER, INTEGER, "5"), new ConditionDto(null, REJECTED, INTEGER, "10")));

        assertThat(updated.versionNumber()).isEqualTo(2);
        assertThat(withoutIds(updated)).containsExactlyInAnyOrder(
                new ConditionDto(null, REJECTED, INTEGER, "10", GT, null),
                new ConditionDto(null, UNDERWRITER, INTEGER, "5", RANGE, "10"));
    }

    @Test
    void explicitOperatorsAreKept() {
        // "7-8 - жёлтая зона, 9+ - отказ" written with the console select
        StopFactorResponse created = create(
                new ConditionDto(null, UNDERWRITER, INTEGER, "7", BETWEEN, "8"), new ConditionDto(null, REJECTED, INTEGER, "9", GTE, null));

        assertThat(withoutIds(created)).containsExactlyInAnyOrder(
                new ConditionDto(null, REJECTED, INTEGER, "9", GTE, null),
                new ConditionDto(null, UNDERWRITER, INTEGER, "7", BETWEEN, "8"));
    }

    @Test
    void everyNumberOperatorCanBeStored() {
        for (ConditionOperator operator : List.of(GT, GTE, LT, LTE, EQ, RANGE, BETWEEN)) {
            String valueTo = operator.hasValueTo() ? "20" : null;
            StopFactorResponse created = create(new ConditionDto(null, REJECTED, INTEGER, "10", operator, valueTo));

            assertThat(withoutIds(created)).containsExactly(new ConditionDto(null, REJECTED, INTEGER, "10", operator, valueTo));
        }
    }

    @Test
    void operatorThatDoesNotFitIsAnError() {
        assertThatThrownBy(() -> create(new ConditionDto(null, REJECTED, INTEGER, "8", IN, null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("conditions[0]: оператор IN (in) не подходит для метрики");
        assertThatThrownBy(() -> consoleService.create(new StopFactorRequest(KIB_REFUSAL_STATUSES_METRIC, "Тест: статусы",
                null, null, true, Map.of(), Set.of(), null,
                List.of(new ConditionDto(null, REJECTED, ARRAY, "WrittenOff", GTE, null)), null), "test"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("conditions[0]: оператор GTE (>=) не подходит для метрики");
    }

    @Test
    void valueToOnlyForRangeAndBetweenAndAboveValue() {
        assertThatThrownBy(() -> create(new ConditionDto(null, REJECTED, INTEGER, "8", GTE, "20")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("conditions[0]: valueTo указывается только для RANGE и BETWEEN, не для GTE");
        assertThatThrownBy(() -> create(new ConditionDto(null, REJECTED, INTEGER, "8", null, "20")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("conditions[0]: valueTo указывается только для RANGE и BETWEEN, не для GT");
        assertThatThrownBy(() -> create(new ConditionDto(null, REJECTED, INTEGER, "8", BETWEEN, null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("conditions[0]: valueTo обязателен для BETWEEN (>= и <=)");
        assertThatThrownBy(() -> create(new ConditionDto(null, REJECTED, INTEGER, "8", RANGE, "8")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("conditions[0]: valueTo (8) должно быть больше value (8)");
        assertThatThrownBy(() -> create(new ConditionDto(null, REJECTED, INTEGER, "8", BETWEEN, "9.5")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("conditions[0]: valueTo: значение должно быть целым числом");
    }

    @Test
    void underwriterThatCanNeverFireIsAnError() {
        // every value above 7 is already >= 5
        assertThatThrownBy(() -> create(new ConditionDto(null, UNDERWRITER, INTEGER, "7", GT, null), new ConditionDto(null, REJECTED, INTEGER, "5", GTE, null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("conditions: UNDERWRITER (> 7) никогда не сработает");
        assertThatThrownBy(() -> create(new ConditionDto(null, UNDERWRITER, INTEGER, "2", LTE, null), new ConditionDto(null, REJECTED, INTEGER, "3", LT, null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("никогда не сработает");
        assertThatThrownBy(() -> create(new ConditionDto(null, UNDERWRITER, INTEGER, "6", BETWEEN, "8"), new ConditionDto(null, REJECTED, INTEGER, "5", RANGE, "8")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("никогда не сработает");
        // legacy thresholds: UNDERWRITER not below REJECTED
        assertThatThrownBy(() -> create(new ConditionDto(null, UNDERWRITER, INTEGER, "8"), new ConditionDto(null, REJECTED, INTEGER, "8")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("никогда не сработает");

        // overlapping but not covered: 3..5 is UNDERWRITER, above 5 REJECTED wins
        assertThat(create(new ConditionDto(null, UNDERWRITER, INTEGER, "3", BETWEEN, "8"), new ConditionDto(null, REJECTED, INTEGER, "5", GT, null)))
                .isNotNull();
        // the same bound included in one and excluded in the other: 5 is UNDERWRITER
        assertThat(create(new ConditionDto(null, UNDERWRITER, INTEGER, "5", EQ, null), new ConditionDto(null, REJECTED, INTEGER, "5", GT, null)))
                .isNotNull();
    }

    @Test
    void sameValueToWrittenDifferentlyIsAccepted() {
        StopFactorResponse created = create(
                new ConditionDto(null, UNDERWRITER, INTEGER, "5", RANGE, "8.0"), new ConditionDto(null, REJECTED, INTEGER, "8"));

        assertThat(withoutIds(created)).contains(new ConditionDto(null, UNDERWRITER, INTEGER, "5", RANGE, "8"));
    }

    @Test
    void calculatorsListTheOperatorsForTheConsoleSelect() throws Exception {
        String json = getJson("/api/v1/admin/metrics/calculators");

        assertThat(JsonPath.<List<Object>>read(json, "$[?(@.type == 'KIB_ACTIVE_DISPUTES')].allowedOperators[*]"))
                .containsExactly(
                        Map.of("code", "GT", "name", ">"),
                        Map.of("code", "GTE", "name", ">="),
                        Map.of("code", "LT", "name", "<"),
                        Map.of("code", "LTE", "name", "<="),
                        Map.of("code", "EQ", "name", "="),
                        Map.of("code", "RANGE", "name", "> и <="),
                        Map.of("code", "BETWEEN", "name", ">= и <="));
        assertThat(JsonPath.<List<Object>>read(json, "$[?(@.type == 'KIB_REFUSAL_STATUSES')].allowedOperators[*]"))
                .containsExactly(Map.of("code", "IN", "name", "in"));
    }

    @Test
    void conditionsCarryTheOperatorCode() throws Exception {
        StopFactorResponse created = create(new ConditionDto(null, UNDERWRITER, INTEGER, "5"), new ConditionDto(null, REJECTED, INTEGER, "8"));

        String json = getJson("/api/v1/admin/stop-factors/" + created.factorId());

        assertThat(JsonPath.<List<Object>>read(json, "$.conditions[?(@.result == 'UNDERWRITER')].operator")).containsExactly("RANGE");
        assertThat(JsonPath.<List<Object>>read(json, "$.conditions[?(@.result == 'UNDERWRITER')].valueTo")).containsExactly("8");
        assertThat(JsonPath.<List<Object>>read(json, "$.conditions[?(@.result == 'REJECTED')].operator")).containsExactly("GT");
    }

    private String getJson(String url) throws Exception {
        return mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------------------------------------
    // Legacy saveCondition
    // ------------------------------------------------------------------------------------------------

    @Test
    void legacySaveConditionMovesTheUnderwriterRange() {
        StopFactorResponse created = create(new ConditionDto(null, UNDERWRITER, INTEGER, "5"), new ConditionDto(null, REJECTED, INTEGER, "8"));

        stopFactorService.saveCondition(new StopFactorConditionDto(created.factorId(), "10", INTEGER, REJECTED));

        assertThat(withoutIds(consoleService.get(created.factorId()))).containsExactlyInAnyOrder(
                new ConditionDto(null, REJECTED, INTEGER, "10", GT, null),
                new ConditionDto(null, UNDERWRITER, INTEGER, "5", RANGE, "10"));
    }

    @Test
    void legacySaveConditionKeepsAnExplicitOperator() {
        StopFactorResponse created = create(
                new ConditionDto(null, UNDERWRITER, INTEGER, "6", BETWEEN, "8"), new ConditionDto(null, REJECTED, INTEGER, "8", GT, null));

        stopFactorService.saveCondition(new StopFactorConditionDto(created.factorId(), "10", INTEGER, REJECTED));

        assertThat(withoutIds(consoleService.get(created.factorId()))).containsExactlyInAnyOrder(
                new ConditionDto(null, REJECTED, INTEGER, "10", GT, null),
                new ConditionDto(null, UNDERWRITER, INTEGER, "6", BETWEEN, "8"));
    }

    // ------------------------------------------------------------------------------------------------
    // Database constraints
    // ------------------------------------------------------------------------------------------------

    @Test
    void databaseAcceptsOnlyConsistentOperators() {
        long versionId = create(new ConditionDto(null, UNDERWRITER, INTEGER, "5"), new ConditionDto(null, REJECTED, INTEGER, "8")).versionId();

        // RANGE and BETWEEN need value_to above value
        assertRejectedByDatabase(versionId, UNDERWRITER, "RANGE", null);
        assertRejectedByDatabase(versionId, UNDERWRITER, "BETWEEN", "5");
        // value_to only for RANGE and BETWEEN, numbers are not IN, operator is known and required
        assertRejectedByDatabase(versionId, UNDERWRITER, "GTE", "8");
        assertRejectedByDatabase(versionId, REJECTED, "IN", null);
        assertRejectedByDatabase(versionId, REJECTED, "NE", null);
        assertRejectedByDatabase(versionId, REJECTED, null, null);
        // any operator for either result
        assertThat(jdbcTemplate.update("""
                UPDATE stop_factors.stop_factor_condition SET operator = 'BETWEEN', value_to = '20'
                WHERE stop_factor_id = ? AND result = 'REJECTED'""", versionId)).isEqualTo(1);
        // the stored conditions themselves are valid
        assertThat(jdbcTemplate.update("""
                UPDATE stop_factors.stop_factor_condition SET operator = operator, value_to = value_to
                WHERE stop_factor_id = ?""", versionId)).isEqualTo(2);
    }

    /** The update fails on a constraint; a savepoint keeps the test transaction usable. */
    private void assertRejectedByDatabase(long versionId, StopFactorStatus result, String operator, String valueTo) {
        jdbcTemplate.execute("SAVEPOINT constraint_check");
        try {
            assertThatThrownBy(() -> jdbcTemplate.update(
                    "UPDATE stop_factors.stop_factor_condition SET operator = ?, value_to = ? WHERE stop_factor_id = ? AND result = ?",
                    operator, valueTo, versionId, result.name()))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.execute("ROLLBACK TO SAVEPOINT constraint_check");
        }
    }

    // ------------------------------------------------------------------------------------------------

    /** Created and flushed, so plain SQL sees the rows. */
    private StopFactorResponse create(ConditionDto... conditions) {
        StopFactorResponse created = consoleService.create(new StopFactorRequest(ACTIVE_DISPUTES_METRIC, "Тест: споры", null,
                null, true, Map.of(), Set.of(), null, List.of(conditions), null), "test");
        entityManager.flush();
        return created;
    }

    private StopFactorResponse update(StopFactorResponse current, List<ConditionDto> conditions) {
        return consoleService.update(current.factorId(), new StopFactorRequest(current.metricId(), current.name(),
                current.description(), current.orderNumber(), current.active(), current.params(), current.productIds(),
                current.segment(), conditions, "Тест операторов"), "test");
    }

    private static List<ConditionDto> withoutIds(StopFactorResponse response) {
        return response.conditions().stream()
                .map(c -> new ConditionDto(null, c.result(), c.valueType(), c.value(), c.operator(), c.valueTo()))
                .toList();
    }

    private long count(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }
}
