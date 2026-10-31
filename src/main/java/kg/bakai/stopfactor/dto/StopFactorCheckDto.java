package kg.bakai.stopfactor.dto;

import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;

public record StopFactorCheckDto(
        long stopFactorId,
        String stopFactorName,
        StopFactorStatus checkResult,
        String checkedValue,
        String rejectedValue,
        StopFactorValueTypes rejectedValueType,
        String underwriterValue,
        StopFactorValueTypes underwriterValueType
) {
}
