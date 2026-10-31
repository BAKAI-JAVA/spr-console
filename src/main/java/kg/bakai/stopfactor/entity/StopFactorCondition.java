package kg.bakai.stopfactor.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stop_factor_condition", schema = "stop_factors")
public class StopFactorCondition extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stop_factor_condition_id_seq")
    @SequenceGenerator(name = "stop_factor_condition_id_seq", sequenceName = "stop_factor_condition_id_generator", schema = "stop_factors", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_factor_id", nullable = false)
    private StopFactor stopFactor;

    @NotNull
    @Column(name = "value", nullable = false, length = Integer.MAX_VALUE)
    private String value;

    @NotNull
    @Column(name = "value_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private StopFactorValueTypes valueType;

    @Column(name = "result")
    @Enumerated(EnumType.STRING)
    private StopFactorStatus result;

}