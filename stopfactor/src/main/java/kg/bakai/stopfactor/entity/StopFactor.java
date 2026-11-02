package kg.bakai.stopfactor.entity;

import jakarta.persistence.*;
import kg.bakai.common.entity.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "stop_factor", schema = "stop_factors")
public class StopFactor extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stop_factor_id_seq")
    @SequenceGenerator(name = "stop_factor_id_seq", sequenceName = "stop_factor_id_generator", schema = "stop_factors", allocationSize = 1)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "order_number")
    private int orderNumber;

    @Column(name = "root_id", updatable = false)
    private Long rootId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber = 1;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "change_reason")
    private String changeReason;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "metric_id", nullable = false)
    private StopFactorMetric metric;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> params = new HashMap<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "stop_factor_product",
            schema = "stop_factors",
            joinColumns = @JoinColumn(name = "stop_factor_id"))
    @Column(name = "product_id", nullable = false)
    private Set<Long> productIds = new HashSet<>();

    public StopFactor(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public boolean isCurrent() {
        return validTo == null;
    }

    public Long getFactorId() {
        return rootId != null ? rootId : id;
    }

    public StopFactor newVersion(LocalDateTime now, String reason) {
        StopFactor next = new StopFactor();
        next.rootId = getFactorId();                 // always the FIRST version, never the previous one
        next.versionNumber = this.versionNumber + 1;
        next.validFrom = now;
        next.changeReason = reason;
        next.name = this.name;
        next.description = this.description;
        next.isActive = this.isActive;
        next.orderNumber = this.orderNumber;
        next.metric = this.metric;
        next.params = new HashMap<>(this.params);
        next.productIds = new HashSet<>(this.productIds);
        return next;
    }
}