package kg.bakai.stopfactor.entity;

import jakarta.persistence.*;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "stop_factor_check", schema = "stop_factors")
public class StopFactorCheck extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stop_factor_check_id_seq")
    @SequenceGenerator(name = "stop_factor_check_id_seq", sequenceName = "stop_factor_check_id_generator", schema = "stop_factors", allocationSize = 1)
    private Long id;

    @Column(name = "scoring_id", nullable = false)
    private Long scoringId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_factor_id", nullable = false)
    private StopFactor stopFactor;

    @Column(name = "checked_value", nullable = false, length = Integer.MAX_VALUE)
    private String checkedValue;

    @Column(name = "result", nullable = false)
    @Enumerated(EnumType.STRING)
    private StopFactorStatus result;

    public StopFactorCheck(long scoringId, StopFactor stopFactor, String checkedValue) {
        this.scoringId = scoringId;
        this.stopFactor = stopFactor;
        this.checkedValue = checkedValue;
        this.result = StopFactorStatus.APPROVED;
    }
}