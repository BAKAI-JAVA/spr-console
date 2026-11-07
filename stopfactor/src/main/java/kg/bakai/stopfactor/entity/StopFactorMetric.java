package kg.bakai.stopfactor.entity;

import kg.bakai.common.entity.BaseEntity;
import kg.bakai.stopfactor.enums.CalculatorType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What a stop factor measures, e.g. "Непрерывная просроченная задолженность": the calculator, the params
 * ({@link MetricParamSchema}) and the name template. Its stop factors are the metric with param values.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "stop_factor_metric", schema = "stop_factors")
public class StopFactorMetric extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stop_factor_metric_id_seq")
    @SequenceGenerator(name = "stop_factor_metric_id_seq", sequenceName = "stop_factor_metric_id_generator", schema = "stop_factors", allocationSize = 1)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "title", unique = true, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculator", nullable = false, updatable = false)
    private CalculatorType calculator;

    @Column(name = "name_template", nullable = false, length = Integer.MAX_VALUE)
    private String nameTemplate;

    /** What the rules of its stop factors are set up with, in console order. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "jsonb", nullable = false)
    private List<MetricParamSchema> params = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Optional<MetricParamSchema> findParam(String key) {
        return params.stream().filter(param -> param.key().equals(key)).findFirst();
    }
}