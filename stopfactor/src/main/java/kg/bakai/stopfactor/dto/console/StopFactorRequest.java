package kg.bakai.stopfactor.dto.console;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record StopFactorRequest(
        @NotNull Long metricId,
        @Size(max = 255) String name,
        String description,
        @Min(0) Integer orderNumber,
        Boolean active,
        Map<String, Object> params,
        Set<@NotNull Long> productIds,
        @NotEmpty List<@NotNull @Valid ConditionDto> conditions,
        @Size(max = 255) String changeReason
) {
}
