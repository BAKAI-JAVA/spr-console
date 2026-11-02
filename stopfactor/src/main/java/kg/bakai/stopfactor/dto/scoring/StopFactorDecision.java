package kg.bakai.stopfactor.dto.scoring;

import kg.bakai.stopfactor.enums.InternalStatusCode;
import kg.bakai.stopfactor.enums.StopFactorStatus;

public record StopFactorDecision(
        StopFactorStatus status,
        InternalStatusCode internalStatusCode
) {
}