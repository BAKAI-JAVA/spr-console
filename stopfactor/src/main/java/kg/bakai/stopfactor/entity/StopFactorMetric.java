package kg.bakai.stopfactor.entity;

import jakarta.persistence.*;
import kg.bakai.common.entity.BaseEntity;
import kg.bakai.stopfactor.dto.console.MetricParamSchema;
import kg.bakai.stopfactor.enums.CalculatorType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "jsonb", nullable = false)
    private List<MetricParamSchema> params = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}