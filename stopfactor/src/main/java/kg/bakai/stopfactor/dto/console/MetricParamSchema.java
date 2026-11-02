package kg.bakai.stopfactor.dto.console;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MetricParamSchema(
        @NotBlank @Size(max = 64) String key,
        @NotBlank @Size(max = 255) String label,
        boolean required
) {
}
