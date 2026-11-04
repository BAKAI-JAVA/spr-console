package kg.bakai.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kg.bakai.common.entity.BaseEntity;
import kg.bakai.common.enums.ProductSegment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Credit product of SPR (public.product). Read only here: products are created and changed by SPR.
 */
@Getter
@Entity
@Immutable
@NoArgsConstructor
@Table(name = "product", schema = "public")
public class Product extends BaseEntity {

    @Id
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    /** Retail or MSB: which segment stop factors check applications of this product. */
    @Enumerated(EnumType.STRING)
    @Column(name = "segment_type", nullable = false, length = 32)
    private ProductSegment segmentType;
}
