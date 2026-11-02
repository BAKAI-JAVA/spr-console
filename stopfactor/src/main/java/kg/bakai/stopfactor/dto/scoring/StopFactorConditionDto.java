package kg.bakai.stopfactor.dto.scoring;

import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;

public record StopFactorConditionDto(
        long stopFactorId,
        String value,
        StopFactorValueTypes valueType,
        StopFactorStatus result
) {
}