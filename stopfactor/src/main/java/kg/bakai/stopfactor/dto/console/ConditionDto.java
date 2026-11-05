package kg.bakai.stopfactor.dto.console;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;

/**
 * A condition of a stop factor version: at most one REJECTED and one UNDERWRITER condition per version.
 *
 * @param value the highest allowed number (INTEGER, DOUBLE) or the loan statuses (ARRAY, STRING)
 */
public record ConditionDto(
        Long id,
        @NotNull StopFactorStatus result,
        @NotNull StopFactorValueTypes valueType,
        @NotBlank String value
) {
}
