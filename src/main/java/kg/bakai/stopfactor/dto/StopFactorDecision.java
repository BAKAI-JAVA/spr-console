package kg.bakai.stopfactor.dto;

import kg.bakai.stopfactor.enums.StopFactorStatus;

public record StopFactorDecision(
        StopFactorStatus status,
        InternalStatusCode internalStatusCode
) {
}