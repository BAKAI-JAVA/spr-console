package kg.bakai.product.dto;

import kg.bakai.common.enums.ProductSegment;

import java.time.LocalDateTime;

/**
 * @param segmentType RETAIL or MSB: which segment stop factors check applications of this product
 */
public record ProductDto(
        Long id,
        String name,
        String description,
        ProductSegment segmentType,
        LocalDateTime createdDate,
        LocalDateTime updatedDate
) {
}
