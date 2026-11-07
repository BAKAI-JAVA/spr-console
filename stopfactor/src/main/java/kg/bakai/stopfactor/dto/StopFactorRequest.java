package kg.bakai.stopfactor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kg.bakai.common.enums.ProductSegment;

import java.util.List;
import java.util.Set;

/**
 * A stop factor in the console: a metric with one or more rules.
 *
 * @param name  left out, a stop factor with one rule is named like its rule, one with several rules like its metric
 * @param rules every rule is checked on its own, e.g. the overdue history of a segment by period and overdue days
 */
public record StopFactorRequest(
        @NotNull Long metricId,
        @Size(max = 255) String name,
        String description,
        @Min(0) Integer orderNumber,
        Boolean active,
        Set<@NotNull Long> productIds,
        ProductSegment segment,
        @NotEmpty List<@NotNull @Valid RuleDto> rules,
        @Size(max = 255) String changeReason
) {
}
