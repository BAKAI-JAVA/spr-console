package kg.bakai.stopfactor.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;

public record ConditionDto(
        Long id,
        @NotNull StopFactorStatus result,
        @NotNull StopFactorValueTypes valueType,
        @NotBlank String value
) {
}
