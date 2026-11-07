package kg.bakai.stopfactor.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kg.bakai.common.entity.BaseEntity;
import kg.bakai.stopfactor.enums.ConditionOperator;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A rule of a stop factor version: the param values its metric is measured with and the conditions that judge the
 * value, e.g. "с 7 по 12 месяц, 6..10 дней: жёлтая зона 6..8, отказ больше 8". Every rule is checked on its own.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "stop_factor_rule", schema = "stop_factors")
public class StopFactorRule extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stop_factor_rule_id_seq")
    @SequenceGenerator(name = "stop_factor_rule_id_seq", sequenceName = "stop_factor_rule_id_generator",
            schema = "stop_factors", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_factor_id", nullable = false, updatable = false)
    private StopFactor stopFactor;

    /** The metric's name template rendered with the params. */
    @Setter
    @Column(name = "name", nullable = false)
    private String name;

    /** An inactive rule is not checked; the stop factor checks its other rules. */
    @Setter
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Order inside the stop factor, from 1. */
    @Column(name = "order_number", nullable = false, updatable = false)
    private int orderNumber;

    /** Values of the metric params, e.g. {"periodFromMonth": 7, "periodToMonth": 12}. */
    @Getter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> params = new LinkedHashMap<>();

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StopFactorCondition> conditions = new ArrayList<>();

    StopFactorRule(StopFactor stopFactor, int orderNumber) {
        this.stopFactor = stopFactor;
        this.orderNumber = orderNumber;
    }

    /** Param values by key as a calculator reads them (Integer or String), in the order of the metric params. */
    public Map<String, Object> getParams() {
        Map<String, Object> ordered = new LinkedHashMap<>();
        stopFactor.getMetric().getParams().stream()
                .map(MetricParamSchema::key)
                .filter(params::containsKey)
                .forEach(key -> ordered.put(key, params.get(key)));
        params.forEach(ordered::putIfAbsent);
        return ordered;
    }

    /** Sets the param values; every key must be a param of the metric of the stop factor, null values are left out. */
    public void setParams(Map<String, ?> params) {
        StopFactorMetric metric = stopFactor.getMetric();
        Map<String, Object> values = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (value != null) {
                MetricParamSchema definition = metric.findParam(key).orElseThrow(() -> new IllegalArgumentException(
                        "Параметр «" + key + "» не задан в метрике «" + metric.getTitle() + "»."));
                values.put(definition.key(), value);
            }
        });
        this.params = values;
    }

    public List<StopFactorCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public StopFactorCondition addCondition(StopFactorStatus result, StopFactorValueTypes valueType, String value,
                                            ConditionOperator operator, String valueTo) {
        StopFactorCondition condition = new StopFactorCondition(this, result, valueType, value, operator, valueTo);
        conditions.add(condition);
        return condition;
    }

    /** The same rule in another version of the stop factor. */
    StopFactorRule copyTo(StopFactor version) {
        StopFactorRule copy = new StopFactorRule(version, orderNumber);
        copy.name = name;
        copy.active = active;
        copy.params = new LinkedHashMap<>(params);
        conditions.forEach(c -> copy.addCondition(c.getResult(), c.getValueType(), c.getValue(), c.getOperator(), c.getValueTo()));
        return copy;
    }
}
