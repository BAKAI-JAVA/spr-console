package kg.bakai.stopfactor.dto.scoring;

import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;

public record StopFactorCheckDto(
        long stopFactorId,
        long stopFactorVersionId,
        int versionNumber,
        String stopFactorName,
        StopFactorStatus checkResult,
        String checkedValue,
        String rejectedValue,
        StopFactorValueTypes rejectedValueType,
        String underwriterValue,
        StopFactorValueTypes underwriterValueType
) {
}
