package kg.bakai.stopfactor.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kg.bakai.stopfactor.enums.CalculatorType;

import java.time.LocalDateTime;
import java.util.List;

public record MetricDto(
        Long id,
        @NotBlank @Size(max = 255) String title,
        @NotNull CalculatorType calculator,
        String calculatorName,
        @NotBlank String nameTemplate,
        List<@NotNull @Valid MetricParamSchema> params,
        Boolean active,
        LocalDateTime createdDate,
        LocalDateTime updatedDate
) {
}
