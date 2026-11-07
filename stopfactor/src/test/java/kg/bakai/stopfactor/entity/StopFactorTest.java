package kg.bakai.stopfactor.entity;

import kg.bakai.stopfactor.enums.ConditionOperator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static kg.bakai.common.enums.ProductSegment.MSB;
import static kg.bakai.common.enums.ProductSegment.RETAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static kg.bakai.stopfactor.enums.StopFactorStatus.REJECTED;
import static kg.bakai.stopfactor.enums.StopFactorStatus.UNDERWRITER;
import static kg.bakai.stopfactor.enums.StopFactorValueTypes.INTEGER;

class StopFactorTest {

    @Test
    void stopFactorWithoutSegmentAndProductsAppliesToEveryApplication() {
        StopFactor stopFactor = new StopFactor("any", null);

        assertThat(stopFactor.appliesTo(7L, RETAIL)).isTrue();
        assertThat(stopFactor.appliesTo(7L, MSB)).isTrue();
        assertThat(stopFactor.appliesTo(null, RETAIL)).isTrue();
    }

    @Test
    void segmentStopFactorAppliesOnlyToItsSegment() {
        StopFactor msb = new StopFactor("msb", null);
        msb.setSegment(MSB);

        assertThat(msb.appliesTo(7L, MSB)).isTrue();
        assertThat(msb.appliesTo(7L, RETAIL)).isFalse();
        assertThat(msb.appliesTo(null, RETAIL)).isFalse();
    }

    @Test
    void productLimitsApplyTogetherWithTheSegment() {
        StopFactor stopFactor = new StopFactor("limited", null);
        stopFactor.setSegment(RETAIL);
        stopFactor.getProductIds().add(5L);

        assertThat(stopFactor.appliesTo(5L, RETAIL)).isTrue();
        assertThat(stopFactor.appliesTo(6L, RETAIL)).isFalse();
        assertThat(stopFactor.appliesTo(5L, MSB)).isFalse();
        assertThat(stopFactor.appliesTo(null, RETAIL)).isTrue();   // scoring without product ignores product limits
    }

    @Test
    void newVersionKeepsTheSegment() {
        StopFactor first = new StopFactor("first", null);
        first.setId(40L);
        first.setMetric(bureauRequestsMetric());
        first.setSegment(MSB);
        first.addRule().setParams(Map.of("days", 30));
        first.getProductIds().add(5L);

        StopFactor next = first.newVersion(LocalDateTime.now(), "reason");

        assertThat(next.getSegment()).isEqualTo(MSB);
        assertThat(next.getRootId()).isEqualTo(40L);
        assertThat(next.getProductIds()).containsExactly(5L);
    }

    @Test
    void newVersionCopiesEveryRuleWithItsParamsAndConditions() {
        StopFactor first = new StopFactor("bureau", null);
        first.setMetric(bureauRequestsMetric());
        StopFactorRule week = first.addRule();
        week.setName("7 дней");
        week.setParams(Map.of("days", 7));
        week.addCondition(REJECTED, INTEGER, "14", ConditionOperator.GT, null);
        StopFactorRule month = first.addRule();
        month.setName("30 дней");
        month.setParams(Map.of("days", 30));
        month.addCondition(UNDERWRITER, INTEGER, "20", ConditionOperator.GT, null);

        StopFactor next = first.newVersion(LocalDateTime.now(), "reason");

        assertThat(next.getRules()).extracting(StopFactorRule::getOrderNumber, StopFactorRule::getName, StopFactorRule::getParams)
                .containsExactly(tuple(1, "7 дней", Map.of("days", 7)), tuple(2, "30 дней", Map.of("days", 30)));
        assertThat(next.getRules()).allSatisfy(rule -> assertThat(rule.getStopFactor()).isSameAs(next));
        assertThat(next.getRules().get(1).getConditions()).singleElement()
                .satisfies(c -> {
                    assertThat(c.getRule()).isSameAs(next.getRules().get(1));
                    assertThat(c.getResult()).isEqualTo(UNDERWRITER);
                    assertThat(c.getValue()).isEqualTo("20");
                });
        // the copies are new rows: the first version keeps its own
        assertThat(next.getRules().getFirst()).isNotSameAs(week);
    }

    @Test
    void paramsAreTypedByTheMetricAndInItsOrder() {
        StopFactorMetric metric = new StopFactorMetric();
        metric.setParams(List.of(new MetricParamSchema("source", "Источник", true),
                new MetricParamSchema("periodFromMonth", "Период с", true), new MetricParamSchema("periodToMonth", "Период по", true)));
        StopFactor stopFactor = new StopFactor("restructurings", null);
        stopFactor.setMetric(metric);
        StopFactorRule rule = stopFactor.addRule();

        rule.setParams(Map.of("periodToMonth", 12, "source", "KIB", "periodFromMonth", 1));

        assertThat(rule.getParams()).containsExactly(
                Map.entry("source", "KIB"), Map.entry("periodFromMonth", 1), Map.entry("periodToMonth", 12));
    }

    @Test
    void aParamOutsideTheMetricIsRefused() {
        StopFactor stopFactor = new StopFactor("bureau", null);
        stopFactor.setMetric(bureauRequestsMetric());

        assertThatThrownBy(() -> stopFactor.addRule().setParams(Map.of("months", 3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Параметр «months» не задан в метрике");
    }

    private static StopFactorMetric bureauRequestsMetric() {
        StopFactorMetric metric = new StopFactorMetric();
        metric.setTitle("Запросы в кредитное бюро");
        metric.setParams(List.of(new MetricParamSchema("days", "Последние N дней", true)));
        return metric;
    }
}
